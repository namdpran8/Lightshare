# Lightshare

Lightshare is an Android app for sending and receiving files visually using a colored grid display. The sender encodes file data into colors on a grid, and the receiver decodes the colors back into data. This project demonstrates a novel approach to data transmission using light/color signals.

## Features

- Select any file to send
- File data is converted into a 16x16 color grid visualization
- Color codes represent 2-bit segments of the file bytes
- Receiver app (planned) to decode color signals back to file data
- Fullscreen immersive UI for better visualization

## How it works

- The sender reads the file bytes
- Each byte is split into 4 pairs of 2 bits
- Each 2-bit pair is mapped to a color:
  - 00 → Black
  - 01 → Red
  - 10 → Green
  - 11 → Blue
- The 16x16 grid displays these colors frame-by-frame to transmit data

## Usage

1. Run the app on your Android device or emulator
2. Click "Send" to pick a file
3. The file's data will be visualized as a colored grid
4. On the receiving device, use the receiver app to capture and decode the colors (work in progress)

## Project Structure

- `MainActivity.kt`: Main UI to select files and show the color grid
- `GridView.kt`: Custom view to convert byte data into color grid and display it
- `ReceiverActivity.kt`: Placeholder for receiving and decoding (to be implemented)

## Future Work

- Implement receiver to decode color signals into original file data
- Improve error correction and synchronization
- Support larger files and dynamic grid sizes
- Add real-time data transmission between physical devices

## Requirements

- Android Studio
- Android device or emulator with minimum SDK 21

## License

MIT License

---

Made with ❤️ by Pranshu Namdeo

