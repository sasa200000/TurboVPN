# OilVPN — راهنمای کامل ساخت APK (فارسی)

این سورس تغییر یافته‌ی MLM VPN است: اسم و رابط به **OilVPN** تغییر کرده، **قفل اشتراک با کد**،
**پنل مدیریت کد ادمین**، لینک تلگرام `@sedef3345` و **نور نئونی هفت‌رنگ** اضافه شده است.

---

## ⚠️ مهم‌ترین نکته

بیلد این پروژه به فایل‌های native (فایل‌های `.so`) نیاز دارد که **در گیت نیستند** (توسط توسعه‌دهنده اصلی حذف شده‌اند). 
این فایل‌ها **داخل APKهای رسمی** انتشار یافته‌ی خود پروژه موجودند:

- https://github.com/mlmvpn/mlmvpn_android/releases
- آخرین نسخه: `v1.2.48`

پس اول باید APK رسمی را دانلود کنید و فایل‌های `.so` را از آن استخراج کنید — یا اینکه از یک نسخه‌ی 
`tar.gz`/`zip` سورس کامل (با LFS) استفاده کنید.

---

## روش ۱: استخراج `.so` از APK رسمی (پیشنهادی — ساده)

### قدم ۱: نصب پیش‌نیازها روی سیستم خودتان

```bash
# جاوا 17+
sudo apt install openjdk-17-jdk git unzip python3-pil

# Android SDK (خط فرمان)
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
```

سپس متغیرهای محیطی:

```bash
export ANDROID_HOME=~/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
export PATH=$ANDROID_HOME/platform-tools:$PATH
```

نصب پلتفرم و build-tools:

```bash
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools" "ndk;27.0.12077973"
```

### قدم ۲: دانلود سورس و APK رسمی

```bash
git clone https://github.com/<YOUR-USER>/<YOUR-REPO>.git oilvpn
cd oilvpn

# APK رسمی (برای استخراج .so)
wget https://github.com/mlmvpn/mlmvpn_android/releases/download/v1.2.48/mlmvpn-1.2.48-arm64-v8a-release.apk -O official.apk
```

### قدم ۳: استخراج `.so` از APK

```bash
mkdir -p apk-extract && cd apk-extract
unzip ../official.apk 'lib/*'
# حالا lib/arm64-v8a/ و lib/armeabi-v7a/ را دارید

# کپی به جای درست در پروژه
mkdir -p ../app/src/main/jniLibs
cp -r lib/* ../app/src/main/jniLibs/
cd ..
```

> نکته: اگر APK رسمی هم دقیقا همین نسخه 1.2.45 باشد بهتر است. اما v1.2.48 هم جواب می‌دهد (فایل‌های
> native بین نسخه‌های کوچک تفاوت چندانی ندارند، ولی سورس شما 1.2.45 است — اگر خطای نسخه گرفتید،
> از release v1.2.45 استفاده کنید).

### قدم ۴: ساخت APK

```bash
./gradlew assembleRelease   # یا assembleDebug برای تست سریع‌تر
```

خروجی: `app/build/outputs/apk/release/app-release-unsigned.apk`

### قدم ۵: امضا

```bash
# ساخت keystore (فقط بار اول)
keytool -genkey -v -keystore oilvpn.keystore -alias oilvpn -keyalg RSA -keysize 2048 -validity 10000

# امضا
apksigner sign --ks oilvpn.keystore --out OilVPN.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

حالا `OilVPN.apk` آماده‌ی نصب است. 🎉

---

## روش ۲: GitHub Actions (بیلد خودکار — نیاز به سیستم شخصی ندارد)

یک ریپوی گیت‌هاب از این سورس بسازید و این فایل را در
`.github/workflows/build.yml` قرار دهید. بعد از هر push، خودش APK می‌سازد و در بخش **Actions** → **Artifacts** قابل دانلود است.

```yaml
name: Build OilVPN APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Download official APK for native libs
        run: |
          wget -q https://github.com/mlmvpn/mlmvpn_android/releases/download/v1.2.48/mlmvpn-1.2.48-arm64-v8a-release.apk -O official.apk
          mkdir -p app/src/main/jniLibs
          cd app/src/main/jniLibs
          unzip -o -q "$GITHUB_WORKSPACE/official.apk" 'lib/*'
          mv lib/* . && rmdir lib

      - name: Build with Gradle
        run: |
          chmod +x gradlew
          ./gradlew assembleRelease --no-daemon -PmlmQuick

      - name: Sign APK (debug key)
        run: |
          $ANDROID_HOME/build-tools/36.0.0/apksigner sign \
            --ks ~/.android/debug.keystore \
            --ks-pass pass:android \
            --out OilVPN.apk \
            app/build/outputs/apk/release/app-release-unsigned.apk || true

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: OilVPN-APK
          path: OilVPN.apk
```

---

## کد اشتراک چطور کار می‌کند؟

- **پنل ادمین**: در اپ، به `تنظیمات → پنل مدیریت کدها` بروید (همه کدها را می‌سازد و کپی می‌دهد)
- **صفحه ورود**: اول اپ، کد اشتراک خواسته می‌شود — بدون آن اپ قفل است
- **لینک تلگرام ادمین** `@sedef3345` روی همان صفحه‌ی ورود است (با نور نئونی)
- **قفل خودکار**: وقتی اشتراک تمام شود، دفعه‌ی بعد که باز کنید دوباره کد می‌خواهد
- کدها: `۳روزه / ۱۴روزه / ۱ماه / ۲ماه / ۳ماه / ۶ماه / ۱ساله`
- کدها به صورت `OIL-XXXXXXXXXX` ساخته می‌شوند و در فایل داخلی اپ ذخیره می‌شوند

## تنظیمات رقص نور

در تنظیمات، بخش نمایش، گزینه‌ی «رقص نور» چند استایل دارد — می‌توانید خاموش کنید یا استایل عوض کنید.

---

## مجوز

سورس اصلی MLM VPN دارای مجوز **GPLv3 + شرط اضافه‌ی ذکر منبع** است.
هر fork باید **mlmvpn** را به عنوان توسعه‌دهنده‌ی اصلی در صفحه‌ی About ذکر کند. نگه‌داشتن صفحه‌ی About الزامی است.
