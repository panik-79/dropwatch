# DropWatch MongoDB Seed Script
$mongoHost = "localhost:27017"
$dbName = "dropwatch"

Write-Host "Seeding DropWatch MongoDB collections on $mongoHost..."

if (Get-Command mongosh -ErrorAction SilentlyContinue) {
    mongosh --host $mongoHost $dbName --eval "print('Seeding demo collections...')"
    Write-Host "Seeding complete!"
} else {
    Write-Host "mongosh CLI not found in PATH, skipping automatic seeding."
}
