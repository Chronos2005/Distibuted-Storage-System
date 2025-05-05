#!/bin/bash

cd "$(dirname "$0")" # Run from script directory

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

# Start Controller in a new terminal
echo "🚀 Starting Controller..."
nohup xfce4-terminal --title="Controller" --hold -e "bash -c 'java -cp bin Controller $C_PORT $R $TIMEOUT $REBALANCE'" > logs/controller_terminal.log 2>&1 &
sleep 5

# Start Dstores in new terminals
echo "🚀 Starting Dstores..."
nohup xfce4-terminal --title="Dstore1" --hold -e "bash -c 'java -cp bin Dstore 2000 $C_PORT $TIMEOUT DStore1'" > logs/dstore1_terminal.log 2>&1 &
sleep 0.5
nohup xfce4-terminal --title="Dstore2" --hold -e "bash -c 'java -cp bin Dstore 2001 $C_PORT $TIMEOUT DStore2'" > logs/dstore2_terminal.log 2>&1 &
sleep 0.5
nohup xfce4-terminal --title="Dstore3" --hold -e "bash -c 'java -cp bin Dstore 2002 $C_PORT $TIMEOUT DStore3'" > logs/dstore3_terminal.log 2>&1 &
sleep 3

# Create a test file if missing
echo "📝 Preparing test file..."
#echo "This is a test file." > to_store/test.txt

# Run ClientMain tests
echo "🧪 Running client tests..."
java -cp client/client.jar:bin ClientMain $C_PORT 1000 "reload" 5

done

echo "✅ Tests completed. Close terminal windows when done."
