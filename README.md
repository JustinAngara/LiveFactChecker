# Generating README for the "Reveal that BS" project
# VIDEO DEMO - https://www.youtube.com/watch?v=6hF1VD-G6fs&feature=youtu.be
readme = """
# Reveal that BS

**Real-time System Audio Fact-Checker**

---

## Overview

**Reveal that BS** (Bad Stats) is a Java-based application that captures your system audio, transcribes spoken content in real time, and instantly verifies statistical claims using the Gemini API. No more pausing, Googling, or second-guessing—you get live feedback with green checks for accurate facts and red flags for misleading or incorrect statistics.

---

## Features

- **System Audio Capture**: Seamlessly listens to any audio playing on your computer (YouTube, news websites, etc.) without extra hardware.
- **Live Transcription**: Uses Google Cloud Speech-to-Text API for accurate, low-latency transcription.
- **Claim Detection & Analysis**: Scans transcripts for objective claims (percentages, figures, dates) and verifies them via the Gemini API.
- **Overlay UI**: Displays an on-screen overlay with color-coded indicators and source links.
- **Diagnostic Report**: Start/Stop buttons generate a detailed report of all flagged claims.
- **Companion Website**: Built with React and Firebase Hosting—includes project info, demo, and an interactive “Spot the BS” game.

---

## Tech Stack

- **Programming Language**: Java (JDK 22)
- **UI Framework**: Swing (JFrame)
- **Speech-to-Text**: Google Cloud Speech-to-Text API (Java client)
- **Fact-Checking AI**: Gemini API for claim verification
- **Frontend Website**: React, Tailwind CSS, TypeScript
- **Hosting & Backend**: Firebase (Hosting, Firestore)
- **Build Tools**: Maven (Java), npm/Yarn (Web)

---

## Prerequisites

1. **Java 22** and Maven  
2. **Google Cloud Project** with Speech-to-Text API enabled  
3. **Gemini API Key**  
4. **Node.js** (>=14) and npm or Yarn  
5. **Firebase CLI** (`npm install -g firebase-tools`)

---

## Installation

### 1. Clone the repository

```bash
git clone https://github.com/JustinAngara/RevealThatBS.git
cd RevealThatBS
