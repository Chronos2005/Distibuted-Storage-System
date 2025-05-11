#!/bin/bash

cd "$(dirname "$0")"  # Run from script directory

# Config
C_PORT=12345
R=2
TIMEOUT=3000
REBALANCE=10

# Ensure folders exist
mkdir -p bin logs DStore1 DStore2 DStore3 to_store downloads

# Compile source files
echo "🛠️ Compiling sources..."
javac src/*.java -d bin || { echo "❌ Compilation failed"; exit 1; }
javac -cp client/client.jar client/ClientMain.java -d bin || { echo "❌ ClientMain compilation failed"; exit 1; }

# Start Controller in background, logging output
echo "🚀 Starting Controller..."
nohup java -cp bin Controller $C_PORT $R $TIMEOUT $REBALANCE \
    > logs/controller.log 2>&1 &
controller_pid=$!

# Give it a moment to initialize
sleep 5

# Start Dstores in background, logging output
echo "🚀 Starting Dstores..."
nohup java -cp bin Dstore 2000 $C_PORT $TIMEOUT DStore1 \
    > logs/dstore1.log 2>&1 &
dstore1_pid=$!
sleep 0.5
nohup java -cp bin Dstore 2001 $C_PORT $TIMEOUT DStore2 \
    > logs/dstore2.log 2>&1 &
dstore2_pid=$!
sleep 0.5
nohup java -cp bin Dstore 2002 $C_PORT $TIMEOUT DStore3 \
    > logs/dstore3.log 2>&1 &
dstore3_pid=$!
sleep 3

# Prepare test file if missing
echo "📝 Preparing test file..."
# Uncomment next line to create a sample file
# echo "This is a test file." > to_store/test.txt

# Run ClientMain tests, logging output
echo "🧪 Running client tests..."
java -cp client/client.jar:bin ClientMain $C_PORT 1000 "concurrentlistduringremove"  \
    > logs/client.log 2>&1

# Kill all background processes
echo "🛑 Killing Controller and DStores..."
kill $controller_pid $dstore1_pid $dstore2_pid $dstore3_pid

echo "✅ Tests completed. Check logs/ for output."
