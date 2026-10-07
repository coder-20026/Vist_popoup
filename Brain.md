# 🧠 Brain.md - Field Work (Field Visit Toolkit) Master Architecture & Blueprint

> **Project Name:** Field Work (Field Visit Toolkit / WhatsApp to Excel)  
> **Repository:** `coder-20026/Notifivisitaistudio00.git`  
> **Platforms:** Web Application (React 18 + Vite + TypeScript + Tailwind CSS) & Native Android App (Kotlin + Jetpack Compose + Apache POI)  
> **Target Audience:** Field Executives, Verification Officers, Collection Agents, Surveyors, Banks & NBFC Field Operations.

---

## 📑 Index (Table of Contents)
1. [Overview & Project Vision](#1-overview--project-vision)
2. [Dual-Stack Architecture (Web + Android)](#2-dual-stack-architecture-web--android)
3. [WhatsApp Chat Parser Engine (Regex & Logic)](#3-whatsapp-chat-parser-engine-regex--logic)
4. [Route Chaining & Geolocation Algorithm](#4-route-chaining--geolocation-algorithm)
5. [Excel Generation Engine (Multi-Sheet Workbook)](#5-excel-generation-engine-multi-sheet-workbook)
   - Sheet Structure & Exact Cell Coordinates
   - Formulas & Calculations (Daily + Monthly)
   - Dynamic Travel KM Rate System
   - Cross-Sheet Summary Table (L20:M21)
6. [PDF Export Engine](#6-pdf-export-engine)
7. [Android Native App Architecture](#7-android-native-app-architecture)
   - Jetpack Compose UI
   - Background GPS Tracking Service
   - Apache POI Integration on Android
8. [CI/CD Workflow (GitHub Actions)](#8-cicd-workflow-github-actions)
9. [Project Directory & File Structure](#9-project-directory--file-structure)
10. [Configuration & Customization Guide](#10-configuration--customization-guide)
11. [Troubleshooting & Edge Cases Guide](#11-troubleshooting--edge-cases-guide)

---

## 1. Overview & Project Vision

Field executives jab daily field visits (loan verification, address verification, collection, field survey) karte hain, tab wo WhatsApp group ya chat me details update bhejte hain. 
Har visit me:
- Bank ka naam aur Verification ka reason
- Applicant ka naam
- Area / Location aur GPS Coordinates (`Latitude, Longitude`)

Is app ka main objective hai:
1. **WhatsApp Chat Export (.txt)** ya raw chat text ko 1-click me ingest aur parse karna.
2. Messages me se relevant cases ko automatically extract karna (Bank, Applicant Name, Reason, Lat/Long, Area).
3. **Route Chain Build Karna:** Executive ke ghar (Home Lat/Long) se pehli visit, pehli se doosri visit, aur aakhri visit se wapas ghar tak ka complete route aur Google Maps link generate karna.
4. **Day-wise Excel Worksheets (.xlsx)** generate karna jo printed expense vouchers ke exact format, row heights, column widths aur formulas ko replicate karti hain.
5. Daily travel allowance (KM Rate), Lunch allowance, Visit count allowance, aur Monthly Grand Total + Mobile Recharge calculate karna.
6. Web browser aur Android mobile application dono me **100% feature parity** provide karna.

---

## 2. Dual-Stack Architecture (Web + Android)

Yeh project do alag-alag layers me perfectly synchronized hai:

```
                  +-----------------------------------+
                  |      Field Work Project Core      |
                  +-----------------------------------+
                                    |
            +-----------------------+-----------------------+
            |                                               |
            v                                               v
+-----------------------+                       +-----------------------+
|   Web Application     |                       |  Native Android App   |
| (React 18 + Vite + TS)|                       | (Kotlin + Compose UI) |
+-----------------------+                       +-----------------------+
| - Lucide Icons        |                       | - Material 3 Compose  |
| - Tailwind CSS v4     |                       | - Apache POI 5.2.5    |
| - ExcelJS Library     |                       | - Android GPS Service |
| - jsPDF Exporter      |                       | - SAF (Storage Access)|
+-----------------------+                       +-----------------------+
            |                                               |
            +-----------------------+-----------------------+
                                    |
                                    v
                  +-----------------------------------+
                  |       GitHub Actions CI/CD        |
                  | (.github/workflows/android.yml)  |
                  | Auto-builds Debug APK on push     |
                  +-----------------------------------+
```

---

## 3. WhatsApp Chat Parser Engine (Regex & Logic)

Chat parser core algorithm WhatsApp chat export files ko scan karta hai aur valid cases ko extract karta hai.

### 3.1 Date & Time Header Regex
WhatsApp ke different versions me date formats (12-hour AM/PM ya 24-hour, single/double digit date, optional comma, optional narrow no-break space `\u202f`) aate hain:
```typescript
const LINE_HEADER_RE =
  /^(\d{1,2})\/(\d{1,2})\/(\d{2,4}),?\s+(\d{1,2}:\d{2}(?::\d{2})?)\s*([apAP][. ]?[mM][.]?)?\s*[-–—]\s*([^:]+?):\s?([\s\S]*)$/
```
- **Multiline Message Stitching:** Agar line is header se match nahi hoti, toh wo previous message ki body me append hoti hai (`messages[messages.length - 1].body += '\n' + line`).
- **Date Parser:** `DD/MM/YYYY` aur `DD/MM/YY` dono ko support karta hai. Agar year < 100 ho toh `+ 2000` add karta hai.

### 3.2 Bank & Reason Extraction
Field cases me reason aur bank aksar bracket format me likhe hote hain, jaise:
`RESIDENCE VERIFICATION (HDFC BANK)` ya `BUSINESS CNV (SBI)`
```typescript
function extractReasonAndBank(body: string): { reason: string; bank: string } | null {
  const bracketMatch = body.match(/^([^\n(]*?)\(([^)]+)\)/m)
  if (!bracketMatch) return null
  const reason = bracketMatch[1].replace(/[:\-–—\s]+$/, '').trim()
  const bank = bracketMatch[2].trim()
  return { reason, bank }
}
```

### 3.3 Applicant Name Extraction
Body me se list numbers (`1)`, `1.`, `-`) strip karne ke baad name match kiya jata hai:
```typescript
const APPLICANT_RE = /applic\w*(?:\s*name)?\s*[:\-–—=]+\s*(.+)/i
```
Matched name ko automatically `Title Case` me convert kiya jata hai (e.g. `RAMESH SHARMA` -> `Ramesh Sharma`).

### 3.4 Geolocation & Area Extraction
Location line `#` marker se start hoti hai:
```typescript
// Line begins with '#'
// Example: # Vrundavan Society, 22.1523, 71.6912 <This message was edited>
const latlongMatch = content.match(/(-?\d+(?:\.\d+)?\s*,\s*-?\d+(?:\.\d+)?)/)
```
- WhatsApp annotations jaise `<This message was edited>` strip kiye jaate hain.
- `Latitude, Longitude` regex se nikalta hai (`latlongTo`).
- Jo remaining text bachta hai, wo `area` ban jata hai.

---

## 4. Route Chaining & Geolocation Algorithm

Har din ki field verification ka ek systematic travel path hota hai:
1. **Home Base Coordinate:** `22.1589,71.6827` (Default Field Executive Home Location).
2. **First Visit:** `latlongFrom` = Home Coordinate (`22.1589,71.6827`), `latlongTo` = Visit 1 Coordinates.
3. **Subsequent Visits ($N$):** `latlongFrom` = Visit ($N-1$) Coordinate, `latlongTo` = Visit $N$ Coordinate.
4. **Return Home Row:** Din ke aakhri visit ke baad ek final return trip add hoti hai:
   - `latlongFrom` = Last Visit Coordinates
   - `latlongTo` = `22.1589,71.6827` (Home)
   - `area` = `<LastArea> - Home`
5. **Google Maps Link Generation:**
   Excel sheet ke column `J` (hidden ya viewable) me Google Maps direction URL automatically inject hota hai:
   ```excel
   =HYPERLINK("https://www.google.com/maps/dir/"&F3&"/"&G3, "Route")
   ```
   Isse click karke exact driving distance aur navigation directly Google Maps me khul jati hai.

---

## 5. Excel Generation Engine (Multi-Sheet Workbook)

Excel sheet printed voucher ke exact standard guidelines ke hisaab se format ki gayi hai.

### 5.1 Worksheet Structure & Layout
- **One Sheet Per Day:** Har din ke liye separate tab banta hai jiska naam date hota hai (e.g. `01-08-2024`, `02-08-2024`).
- **Sheet Name Sanitization:** Excel rules ke mutabik `/` invalid hota hai, isliye `DD/MM/YYYY` ko `DD-MM-YYYY` me convert kiya jata hai (max 31 characters).
- **Columns (A to I):**
  - `A`: SR NO. (Width: `6.71`)
  - `B`: BANK NAME (Width: `12.43`)
  - `C`: APPLICAT NAME (Width: `33.86`)
  - `D`: STATUS (Width: `5.86`)
  - `E`: REASON FOR CNV (Width: `8.43`)
  - `F`: LATLONG FROM. (Width: `18.43`)
  - `G`: LATLONG TO. (Width: `19.00`)
  - `H`: AREA (Width: `17.86`)
  - `I`: KM (Width: `7.57`)
  - `J`: MAP (Width: `13.00`)

### 5.2 Header Section (Row 1 & 2)
- **A1:F1 (Merged):** `FIELD EXECUTIVE NAME :- <Executive Name>` (16pt Bold, Left-aligned). Agar name blank ho toh `FIELD EXECUTIVE NAME :-` display hota hai.
- **G1:I1 (Merged):** `DATE :- <DD/MM/YYYY>` (16pt Bold, Right-aligned).
- **Row 2:** Table Headers (11pt Bold, Centered, Thin Borders, Gray Fill `#F2F2F2`).
- **Rows 3 to 17:** Exactly 15 data rows reserve hoti hain standard voucher layout maintain karne ke liye. Blank rows me bhi thin border format rehta hai.
- **Row 18:** Empty spacer row (Height: `16.5pt`).

### 5.3 Daily Summary Table (F19:H24)
Har din ke voucher ke neeche F19 se H24 tak daily calculation table hoti hai:
- **Row 19:** `G19` = `NO. OF COUNT`, `H19` = `AMOUNT` (Bold Header)
- **Row 20 (Total KM):**
  - `F20` = `Total KM`
  - `G20` = `=SUM(I3:I17)&"×<kmRate>"` (Formula: Total KM aur rate ka label)
  - `H20` = `=ROUNDUP(SUM(I3:I17)*<kmRate>,0)` (Formula: Rounded amount)
- **Row 21 (Lunch Allowance):**
  - `F21` = `LUNCH`
  - `G21` = Empty
  - `H21` = `=IF(SUM(I3:I17)>=110,75,0)` (Rule: Agar daily travel >= 110 KM ho toh ₹75 lunch allowance, warna ₹0)
- **Row 22 (Visit Count Allowance):**
  - `F22` = `VISIT`
  - `G22` = `=COUNTA(B3:B17)&"×25"` (Number of visits multiplied by ₹25)
  - `H22` = `=COUNTA(B3:B17)*25`
- **Row 23 (Daily Total):**
  - `F23` = `TOTAL`
  - `H23` = `=SUM(H20:H22)` (Sum of KM Allowance + Lunch + Visits)
- **Row 24 (Advance Deduction):**
  - `F24` = `ADVANCE`
  - `H24` = User advance amount placeholder

### 5.4 Monthly Grand Total (Workbook ke Last Sheet par)
Month end consolidated bill ke liye, **last sheet** par monthly calculations attach hoti hain:
- **F22:** `GRAND TOTAL`
- **G22:** `=SUM('01-08-2024'!H23+'02-08-2024'!H23+...)` (Har din ke H23 cell ka dynamic sum)
- **F23:** `MOBILE RECHARGE`
- **G23:** Fixed Allowance `250` (₹250 Mobile Recharge)
- **H23:** `=G22+G23` (Grand Total + Mobile Recharge)

### 5.5 Cross-Sheet Summary Table (L20:M21 - Last Sheet Only)
Voucher print area ke bahar (Column L aur M) ek ultra-handy analytical summary table add ki gayi hai:
- **L20:** `Total KM` (14pt Bold, Center Aligned)
- **M20:** `=SUM('01-08-2024'!I3:I17)+SUM('02-08-2024'!I3:I17)+...` (Month ke saare sheets ke Total KM ka sum)
- **L21:** `Total Visit` (14pt Bold, Center Aligned)
- **M21:** `=COUNTA('01-08-2024'!B3:B17)+COUNTA('02-08-2024'!B3:B17)+...` (Month ke saare sheets ke Total Visits ka sum)
- **Styling:** Thick outline box, clean borders, non-interfering with A1:I24 print area.

---

## 6. PDF Export Engine

Web application me `excel-to-pdf.ts` module direct client-side high-resolution PDF generation handle karta hai:
- **Renderer:** `jsPDF` vector rendering engine.
- **Print Scope:** Exactly `A1:I24` bounding box render hota hai. Column L & M ki extra tables print me exclude rehti hain taaki A4 sheet clean rahe.
- **Page Geometry:** Standard A4 page, exact margins, crisp borders aur typography hierarchy preserve rehti hai.
- **Multi-page Output:** Agar multiple dates hain toh har date ka daily voucher separate page par neatly format hota hai.

---

## 7. Android Native App Architecture

Android application `/android` directory me completely modern Jetpack Compose stack par bani hui hai.

### 7.1 Tech Stack & Dependencies
- **UI Framework:** Android Jetpack Compose with Material 3.
- **Programming Language:** Kotlin (with Coroutines & Flow).
- **Excel Engine:** `org.apache.poi:poi` & `org.apache.poi:poi-ooxml:5.2.5`.
- **Target SDK:** Android 34 (Android 14) / Min SDK: Android 26 (Android 8.0 Oreo).
- **Architecture:** MVVM Pattern (Unidirectional Data Flow).

### 7.2 Core Files & Responsibilities
| File Name | Responsibility |
|---|---|
| `MainActivity.kt` | App entry point, Permission requests, File Choosers, Intent routing, Toast/Share actions. |
| `ui/screens/MainScreen.kt` | Pure Jetpack Compose UI: File upload card, Raw text input, Settings dialog (Executive name, KM rate, Sender filter), Data Table with live editing. |
| `parser/ChatParser.kt` | Native Kotlin port of the chat parsing logic, Regex engine, and date range filters. |
| `exporter/ExcelExporter.kt` | Apache POI implementation generating multi-sheet `.xlsx` files with exact cell formatting and formulas. |
| `GpsNotificationService.kt` | Foreground location tracking service with ongoing notification. |
| `GpsStateReceiver.kt` | Broadcast receiver listening to `PROVIDERS_CHANGED` to detect if user turns GPS on/off. |
| `res/values/strings.xml` | App Name string resource (`<string name="app_name">Field Work</string>`). |

### 7.3 Background GPS Tracking Service
Field agents ki live location track karne ke liye background location service shamil hai:
- **Foreground Service:** `GpsNotificationService` ongoing notification ke sath chalti hai taaki Android OS isse kill na kare.
- **Permissions:** `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`.
- **Notification Updates:** Status bar me latest latitude aur longitude real-time dikhta rehta hai.

---

## 8. CI/CD Workflow (GitHub Actions)

Root ke andar `/.github/workflows/android.yml` CI/CD file configured hai jo GitHub par code push hote hi automatically APK build karti hai:

```yaml
name: Android Build & Release
on:
  push:
    branches: [ master, main ]
  pull_request:
    branches: [ master, main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - Setup JDK 17 (Eclipse Temurin)
      - Setup Android SDK v3
      - Auto-locate Gradle project root (finds /android folder)
      - Setup Gradle Wrapper (v8.9.0 wrapper jar auto-download)
      - Run: ./gradlew assembleDebug --no-daemon --stacktrace
      - Upload Artifact: app-debug.apk (30 days retention)
```

### GitHub Actions Se Nayi APK Kaise Download Karein:
1. GitHub repo par code push karo (`git push origin master`).
2. GitHub repository ke **Actions** tab me jao.
3. Latest workflow run par click karo (`Android Build & Release`).
4. Build complete hone ke baad bottom me **Artifacts** section me `app-debug` zip file mil jayegi jisme fresh APK rehti hai.

---

## 9. Project Directory & File Structure

```
/
├── .github/
│   └── workflows/
│       └── android.yml                # GitHub Actions automated APK build pipeline
├── android/                           # Complete Native Android Studio project
│   ├── app/
│   │   ├── build.gradle.kts           # App level dependencies (Compose, POI, Coroutines)
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml    # Permissions & Services registration
│   │   │   ├── java/com/whatsapptoexcel/app/
│   │   │   │   ├── MainActivity.kt    # Main activity & state handlers
│   │   │   │   ├── GpsNotificationService.kt # Foreground GPS tracker
│   │   │   │   ├── GpsStateReceiver.kt # GPS On/Off broadcast listener
│   │   │   │   ├── exporter/
│   │   │   │   │   ├── ExcelExporter.kt # Apache POI Excel generator
│   │   │   │   │   └── PdfConverter.kt  # Android PDF exporter
│   │   │   │   ├── parser/
│   │   │   │   │   └── ChatParser.kt    # Kotlin WhatsApp chat parser
│   │   │   │   └── ui/
│   │   │   │       ├── screens/MainScreen.kt # Full Jetpack Compose interface
│   │   │   │       └── theme/         # Colors, Typography, Shapes
│   │   │   └── res/
│   │   │       └── values/strings.xml # App branding ("Field Work")
│   │   └── proguard-rules.pro
│   ├── build.gradle.kts               # Top-level Gradle configuration
│   ├── gradle.properties
│   └── settings.gradle.kts
├── src/                               # Web Frontend (React + Vite + TypeScript)
│   ├── App.tsx                        # Master UI Component with responsive cards
│   ├── components/                    # UI Components (Alerts, Tables, Modals)
│   ├── lib/
│   │   ├── chat-parser.ts             # TypeScript WhatsApp parser engine
│   │   ├── excel-export.ts            # ExcelJS export engine (identical format)
│   │   ├── excel-to-pdf.ts            # jsPDF pixel-perfect A4 voucher exporter
│   │   └── utils.ts                   # Class merge utilities (cn)
│   ├── main.tsx                       # React DOM entry point
│   └── index.css                      # Tailwind CSS v4 styling rules
├── package.json                       # Node dependencies (exceljs, jspdf, lucide-react)
├── vite.config.ts                     # Vite build & dev server config
├── metadata.json                      # AI Studio application metadata
└── Brain.md                           # This Master Documentation File
```

---

## 10. Configuration & Customization Guide

### Executive Name
- **Settings Card / Dialog:** Web aur Android dono me Settings khol kar "Field Executive Name" daal sakte hain.
- **Result:** Har din ki sheet ke top header me `FIELD EXECUTIVE NAME :- <Name>` print hoga. Agar empty ho toh sirf `FIELD EXECUTIVE NAME :-` rahega bina error ke.

### Travel KM Rate
- **Default Rate:** `2.5` (₹2.50 per KM).
- **Customization:** Settings me `Travel Rate (KM Rate)` field me koi bhi number (e.g. `2`, `2.5`, `3`, `5`) enter kar sakte hain.
- **Dynamic Formula:** Excel me cell G20 aur H20 dynamically update hote hain:
  - `G20`: `=SUM(I3:I17)&"×<kmRate>"`
  - `H20`: `=ROUNDUP(SUM(I3:I17)*<kmRate>,0)`

### Base Home Location
- Default Home Latitude/Longitude `22.1589,71.6827` set hai.
- Code level par change karne ke liye:
  - **Web:** `src/lib/excel-export.ts` me `const HOME_LATLONG = '...'` edit karein.
  - **Android:** `android/.../ExcelExporter.kt` me `const val HOME_LATLONG = "..."` edit karein.

---

## 11. Troubleshooting & Edge Cases Guide

| Problem | Root Cause | Solution |
|---|---|---|
| **Chat upload karne par 0 cases dikh rahe hain** | Sender name filter match nahi ho raha ya Date range out of bounds hai. | Settings me jakar `WhatsApp naam (sender filter)` check karo. Wo exact wahi hona chahiye jo export me dikhta hai (e.g., `Chauhan`). Date range filter ko check karein. |
| **Excel formula me `#NAME?` ya `#VALUE!` aa raha hai** | Excel localized language (jaise Hindi ya Spanish) use kar raha hai ya quotes escape nahi hue. | Formulas standard uppercase English me likhe gaye hain (`SUM`, `ROUNDUP`, `IF`, `COUNTA`). Standard MS Excel ya WPS Office use karein. |
| **Android APK me purana app name dikh raha hai** | GitHub Actions me push ke baad naya build download nahi kiya. | Code push hone ke baad GitHub Actions tab me check karein ki naya workflow pass hua ya nahi, aur fresh `app-debug.apk` download karke install karein. |
| **Location allow nahi ho rahi Android par** | Android 14 me foreground service type permission required hoti hai. | `AndroidManifest.xml` me `FOREGROUND_SERVICE_LOCATION` add hai. Phone settings me app ko Location permission "Allow all the time" ya "While using app" grant karein. |
| **PDF export me text cut ho raha hai** | Browser print scaling issue. | Application ka custom `excel-to-pdf.ts` engine direct vector canvas banata hai bina browser print dialog ke, isliye A4 standard par exact fit hota hai. |

---

*Last Updated: 2026-09-05*  
*Maintained by: Antigravity AI Engine & Field Work Core Engineering Team.*
