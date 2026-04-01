Chạy lệnh này để nạp biến từ .env và chạy app

```
Get-Content .env | ForEach-Object { if ($_ -match "^(.*?)=(.*)$") { [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }; ./mvnw spring-boot:run
```

Truy cập:
```
http://localhost:8080/swagger-ui/index.html
```