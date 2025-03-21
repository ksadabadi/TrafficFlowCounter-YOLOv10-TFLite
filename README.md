# Real-Time Object Counting App

## Overview
This project is a **real-time object counting application** developed for traffic analysis. It detects and counts vehicles and pedestrians crossing a predefined line in **four possible directions**:
- **Left to Right**
- **Right to Left**
- **Top to Bottom**
- **Bottom to Top**

The application uses **YOLO v10**, converted to **TensorFlow Lite (TFLite)** for efficient mobile inference. It is developed using **Android Studio** and employs advanced tracking algorithms to ensure accurate counting.

## Features
- **Real-time detection and tracking** of:
  - Cars
  - Trucks
  - Bikes
  - Pedestrians
- **Four directional counting options**
- **Optimized for mobile devices** using **TFLite**
- **Uses SORT algorithm** (Simple Online and Realtime Tracker) with:
  - **Kalman Filter** for motion prediction
  - **Hungarian Algorithm** for object association

## Algorithms Used
### **1. YOLO v10 (You Only Look Once - Version 10)**
YOLO v10 is a deep learning-based object detection model that processes images in a single pass. It divides an image into a grid and predicts bounding boxes and class probabilities simultaneously, making it highly efficient for real-time applications.

### **2. SORT (Simple Online and Realtime Tracker)**
SORT is a lightweight tracking algorithm that uses:
- **Kalman Filter** to predict object movement based on past observations.
- **Hungarian Algorithm** to match detected objects with existing tracks efficiently.

### **3. Kalman Filter**
The **Kalman Filter** is a mathematical algorithm used for predicting an object's future state (position, velocity) by considering:
- **Previous state** (position, velocity, etc.)
- **Current measurement** (from YOLO detection)
- **Noise and uncertainty** in the system

This helps in **smoothing object motion**, reducing detection errors, and handling occlusions.

### **4. Hungarian Algorithm**
The **Hungarian Algorithm** is used for assigning detections to tracked objects optimally. It solves the **assignment problem** by minimizing the total cost of object-detection matching.

## Installation
### **Prerequisites**
- Android Studio
- TensorFlow Lite (TFLite) support
- YOLO v10 TFLite model

### **Steps to Build and Run**
1. Clone the repository:
   ```sh
   git clone https://github.com/your-repository-url.git
   ```
2. Open the project in **Android Studio**.
3. Build and run the app on an **Android device**.
4. Select a **counting direction** and start detection.

## Project Details
This project was developed for **The FEDS** by the **Center for Advanced Transportation Technology (CATT)**.

### **Developed By:**
**Jeffy Lauren Raj**

### **CATT Lab Link:**  
[Center for Advanced Transportation Technology (CATT)](https://cee.umd.edu/research/center-advanced-transportation-technology-laboratory)

### **Contact**
📧 Email: jeffy22@umd.edu


