# 🚀 Hướng dẫn Cài đặt Backend và Frontend với Docker

**MyQuizz Application** - Ứng dụng Quiz Real-time

---

## 📋 Mục lục

1. [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
2. [Chuẩn bị ban đầu](#-chuẩn-bị-ban-đầu)
3. [Cấu hình môi trường](#-cấu-hình-môi-trường)
4. [Chạy ứng dụng với Docker Compose](#-chạy-ứng-dụng-với-docker-compose)
5. [Kiểm tra trạng thái dịch vụ](#-kiểm-tra-trạng-thái-dịch-vụ)
6. [Dừng ứng dụng](#-dừng-ứng-dụng)
7. [Xử lý sự cố](#-xử-lý-sự-cố)
8. [Lệnh hữu ích](#-lệnh-hữu-ích)
9. [Database Migration](#-database-migration--seeding)
10. [Kiểm tra sơ bộ](#-kiểm-tra-sơ-bộ-sau-khi-cài-đặt)

> **💡 Ghi chú:** Nếu click vào link TOC không hoạt động, bạn có thể cuộn xuống để xem từng section.

---

## 🔧 Yêu cầu hệ thống

### Bắt buộc cài đặt
- **Docker**: Phiên bản 20.10+ 
  - Download: https://www.docker.com/products/docker-desktop
- **Docker Compose**: Phiên bản 2.0+
  - Thường được cài kèm với Docker Desktop

### Kiểm tra cài đặt

Mở terminal/PowerShell và chạy các lệnh sau:

```powershell
# Kiểm tra Docker
docker --version
# Kết quả: Docker version 20.10.x hoặc cao hơn

# Kiểm tra Docker Compose
docker compose version
# Kết quả: Docker Compose version 2.x.x hoặc cao hơn
```

Nếu chưa cài đặt, vui lòng tải Docker Desktop từ link trên.

---

## 📦 Chuẩn bị ban đầu

### Bước 1: Kiểm tra cấu trúc thư mục

Đảm bảo bạn đang ở thư mục gốc của dự án:

```
MyQuizApp/
├── server/
│   ├── backend/          # Source code backend
│   ├── frontend/         # Source code frontend
│   ├── docker-compose.yml
│   └── .env              # File cấu hình (chúng ta sẽ tạo)
├── app/                  # Android app
└── doc/                  # Tài liệu
```

### Bước 2: Chuyển đến thư mục server

```powershell
cd MyQuizApp/server
```

Kiểm tra các file cần thiết:
- `docker-compose.yml` ✓
- `backend/Dockerfile` ✓
- `backend/package.json` ✓
- `frontend/Dockerfile` ✓
- `frontend/package.json` ✓

---

## 🔐 Cấu hình môi trường

### Bước 1: Tạo file .env

Từ thư mục `MyQuizApp/server`, tạo file `.env`:

```powershell
# Windows PowerShell
New-Item -Name ".env" -ItemType File

# Hoặc: tạo file thủ công qua text editor
```

### Bước 2: Thêm cấu hình vào file .env

Mở file `.env` vừa tạo và thêm nội dung sau:

```env
# ========== SERVER CONFIG ==========
PORT=3000
NODE_ENV=development

# ========== DATABASE CONFIG ==========
# PostgreSQL
DB_USER=quizuser
DB_PASSWORD=quizpass123
DB_NAME=myquizz_db
DB_HOST=postgres
DB_PORT=5432

# ========== REDIS CONFIG ==========
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=redispass123

# ========== JWT CONFIG ==========
JWT_SECRET=your-secret-key-change-this-in-production
JWT_EXPIRE=7d

# ========== AWS S3 CONFIG (Tuỳ chọn) ==========
AWS_REGION=ap-southeast-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
S3_BUCKET_NAME=myquizz-bucket

# ========== FRONTEND CONFIG ==========
VITE_API_URL=http://localhost:3000
VITE_WS_URL=ws://localhost:3000

# ========== EMAIL CONFIG (Tuỳ chọn) ==========
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# ========== LOG CONFIG ==========
LOG_LEVEL=debug
```

⚠️ **Lưu ý quan trọng:**
- Thay đổi `JWT_SECRET` thành một giá trị an toàn cho production
- Nhập thông tin AWS S3 nếu muốn sử dụng file upload
- File `.env` chứa thông tin nhạy cảm, KHÔNG thêm vào Git
- File `.env` được liệt kê trong `.gitignore` rồi

---

## 🐳 Chạy ứng dụng với Docker Compose

### Bước 1: Build các container

Trong thư mục `MyQuizApp/server`, chạy lệnh:

```powershell
docker compose build
```

Lệnh này sẽ:
- Tải base images (Node.js, PostgreSQL, Redis)
- Build backend application
- Build frontend application

**Thời gian chạy:** 5-10 phút (lần đầu)

### Bước 2: Khởi động tất cả dịch vụ

```powershell
docker compose up -d
```

Cờ `-d` nghĩa là chạy ở chế độ nền (detached mode).

**Output kỳ vọng:**
```
✔ Network app-network Created
✔ Volume postgres_data Created
✔ Volume redis_data Created
✔ Container postgres Started
✔ Container redis Started
✔ Container backend Started
✔ Container frontend Started
```

### Bước 3: Chờ dịch vụ khởi động hoàn tất

Các dịch vụ có healthcheck, hãy đợi khoảng 30-60 giây để chúng hoàn tất khởi động.

Kiểm tra trạng thái:

```powershell
docker compose ps
```

**Output kỳ vọng:**
```
NAME              STATUS           PORTS
postgres          healthy          5432/tcp
redis             healthy          6379/tcp
backend           healthy (running) 3000:3000/tcp
frontend          Up                3001:3001/tcp
```

### Bước 4: Truy cập ứng dụng

- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:3000
- **Swagger API Docs**: http://localhost:3000/api-docs

---

## 📊 Kiểm tra trạng thái dịch vụ

### Xem logs của tất cả dịch vụ

```powershell
# Xem logs theo thời gian thực
docker compose logs -f

# Xem logs của backend
docker compose logs -f backend

# Xem logs của frontend
docker compose logs -f frontend

# Xem 50 dòng log cuối cùng
docker compose logs --tail=50
```

### Kiểm tra container đang chạy

```powershell
# Xem tất cả container
docker compose ps

# Xem chi tiết một container
docker compose exec backend ps aux
```

### Kiểm tra kết nối database

```powershell
# Truy cập PostgreSQL container
docker compose exec postgres psql -U quizuser -d myquizz_db

# Một số lệnh SQL hữu ích:
# \dt                    # Danh sách tất cả bảng
# \d <table_name>       # Mô tả cấu trúc bảng
# SELECT * FROM users;  # Truy vấn dữ liệu
# \q                    # Thoát
```

### Kiểm tra Redis cache

```powershell
# Truy cập Redis container
docker compose exec redis redis-cli -a redispass123

# Các lệnh Redis hữu ích:
# INFO              # Thông tin Redis
# KEYS *            # Liệt kê tất cả keys
# GET <key>         # Lấy giá trị key
# DBSIZE            # Số keys trong DB
# FLUSHDB           # Xóa tất cả keys
# EXIT              # Thoát
```

---

## 🛑 Dừng ứng dụng

### Dừng tất cả dịch vụ nhưng giữ dữ liệu

```powershell
docker compose stop
```

**Lợi ích:** Dữ liệu PostgreSQL và Redis vẫn được lưu trong volumes.

### Dừng và xóa tất cả container

```powershell
docker compose down
```

### Dừng và xóa tất cả, kể cả dữ liệu

```powershell
# ⚠️ CẢNH BÁO: Điều này sẽ xóa tất cả dữ liệu!
docker compose down -v
```

---

## 🐛 Xử lý sự cố

### Vấn đề: Port đã được sử dụng

**Triệu chứng:** 
```
Error: bind: address already in use
```

**Giải pháp:**

```powershell
# Cách 1: Tìm và dừng process chiếm port
# Windows - Tìm process trên port 3000
netstat -ano | findstr :3000
# Sau đó dừng task theo PID
taskkill /PID <PID> /F

# Cách 2: Đổi port trong docker-compose.yml
# Thay đổi "3000:3000" thành "3002:3000"
```

### Vấn đề: Backend không kết nối được database

**Triệu chứng:**
```
Error: connect ECONNREFUSED 127.0.0.1:5432
```

**Giải pháp:**

```powershell
# 1. Kiểm tra container database đang chạy
docker compose ps

# 2. Kiểm tra logs của database
docker compose logs postgres

# 3. Khởi động lại dịch vụ
docker compose restart postgres

# 4. Chờ healthcheck pass
docker compose ps  # Xem status
```

### Vấn đề: Memory không đủ

**Triệu chứng:**
```
OOMKilled / Cannot allocate memory
```

**Giải pháp:**

```powershell
# Dọn dẹp hệ thống
docker system prune -a

# Tăng memory cho Docker (Windows):
# Settings > Resources > Memory slider
```

### Vấn đề: Frontend hiển thị trang trắng

**Nguyên nhân:** API backend không thể truy cập

**Giải pháp:**

```powershell
# 1. Kiểm tra backend logs
docker compose logs backend

# 2. Kiểm tra URL API trong file .env
# VITE_API_URL=http://localhost:3000

# 3. Xây dựng lại frontend
docker compose down
docker compose build frontend
docker compose up -d frontend
```

### Vấn đề: Backend crash với "Invalid environment variables"

**Triệu chứng:**
```
Invalid environment variables: {
  JWT_EXPIRES_IN: { _errors: [ 'Invalid input: expected string, received undefined' ] },
  JWT_REFRESH_SECRET: { _errors: [ ... ] },
  ...
}
```

**Nguyên nhân:** 
- File `.env` chưa được tạo
- File `.env` không có toàn bộ biến môi trường cần thiết
- Không cd vào thư mục `server/` trước khi chạy `docker compose`

**Giải pháp:**

```powershell
# 1. Chuyển vào thư mục server
cd server

# 2. Kiểm tra file .env tồn tại
Test-Path .env   # Kết quả: True

# 3. Đảm bảo file .env có tất cả biến bắt buộc (xem phần "Cấu hình môi trường")

# 4. Build lại backend
docker compose build --no-cache backend

# 5. Khởi động lại backend
docker compose up -d backend

# 6. Kiểm tra logs
docker compose logs backend --tail=20
```

**Lưu ý quan trọng:**
- Luôn `cd server` trước khi chạy `docker compose` commands
- File `.env` phải nằm trong thư mục `server/`
- Sau khi thay đổi `.env`, cần build lại backend: `docker compose build --no-cache backend`

---

## 📝 Lệnh hữu ích

### Quản lý container

```powershell
# Khởi động lại một dịch vụ
docker compose restart backend

# Khởi động một dịch vụ cụ thể
docker compose start backend

# Dừng một dịch vụ cụ thể
docker compose stop backend

# Xem cấu trúc network
docker network inspect app-network
```

### Làm sạch và xóa

```powershell
# Xóa images không sử dụng
docker image prune

# Xóa volumes không sử dụng
docker volume prune

# Xóa toàn bộ (containers, images, volumes)
docker system prune -a --volumes
```

### Build lại images

```powershell
# Build lại tất cả
docker compose build --no-cache

# Build lại backend
docker compose build --no-cache backend

# Build lại frontend
docker compose build --no-cache frontend
```

### Xem chi tiết container

```powershell
# Xem thông tin chi tiết một container
docker compose inspect backend

# Xem resource usage
docker stats

# Xem logs real-time
docker compose logs -f --tail=100
```

---

## 🔄 Database Migration & Seeding

### Chạy migration

Nếu backend có database migration:

```powershell
# Truy cập vào backend container
docker compose exec backend npm run db:migrate
```

### Seed dữ liệu mẫu

```powershell
docker compose exec backend npm run db:seed
```

### Chạy cả hai

```powershell
docker compose exec backend npm run db:migrate:seed
```

---

## ✅ Kiểm tra sơ bộ sau khi cài đặt

Chạy những kiểm tra sau để đảm bảo mọi thứ hoạt động:

```powershell
# 1. Kiểm tra tất cả container đang chạy
docker compose ps

# 2. Kiểm tra kết nối backend
curl http://localhost:3000/health

# 3. Mở frontend
# Vào http://localhost:3001 trong trình duyệt

# 4. Kiểm tra API docs
# Vào http://localhost:3000/api-docs

# 5. Kiểm tra database
docker compose exec postgres psql -U quizuser -d myquizz_db -c "SELECT 1;"
```

---

## 📚 Thông tin thêm

### Cấu trúc dịch vụ

**PostgreSQL** (Port 5432)
- Database chính cho ứng dụng
- Volume: `postgres_data`
- Healthcheck: Kiểm tra mỗi 10 giây

**Redis** (Port 6379)
- Cache và session storage
- Volume: `redis_data`
- Healthcheck: Kiểm tra mỗi 10 giây

**Backend** (Port 3000)
- API server Express.js + TypeScript
- Phụ thuộc: PostgreSQL, Redis
- Healthcheck: HTTP GET /health mỗi 10 giây

**Frontend** (Port 3001)
- Ứng dụng React + Vite
- Phụ thuộc: Backend
- Serve bằng `serve` package

### Network

- Tất cả dịch vụ kết nối qua bridge network: `app-network`
- Services có thể truy cập nhau bằng tên (ví dụ: `postgres`, `redis`, `backend`)

---

## 🎯 Bước tiếp theo

1. ✅ Cài đặt Docker & Docker Compose
2. ✅ Cấu hình file `.env`
3. ✅ Chạy `docker compose up -d`
4. ✅ Kiểm tra ứng dụng ở http://localhost:3001
5. 📋 Phát triển ứng dụng

---

## ❓ Câu hỏi thường gặp

**Q: Làm sao để thay đổi port?**
A: Chỉnh sửa trong `docker-compose.yml`. Ví dụ: `"3002:3000"` thay cho `"3000:3000"`

**Q: Dữ liệu database có được lưu không?**
A: Có, được lưu trong Docker volume `postgres_data`. Xóa volume nếu chạy `docker compose down -v`

**Q: Làm sao để update code?**
A: Chỉnh sửa code trong thư mục `backend/` hoặc `frontend/`, sau đó build lại:
```
docker compose build
docker compose up -d
```

**Q: Có thể chạy local mà không dùng Docker không?**
A: Có thể, nhưng cần cài Node.js, PostgreSQL, Redis cục bộ. Docker Compose dễ hơn.

---

**Cập nhật lần cuối:** 2026-07-21
**Phiên bản:** 1.0
