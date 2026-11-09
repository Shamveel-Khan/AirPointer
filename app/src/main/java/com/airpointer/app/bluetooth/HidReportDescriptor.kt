package com.airpointer.app.bluetooth

object HidReportDescriptor {
    const val REPORT_ID_MOUSE: Byte = 1
    const val REPORT_ID_KEYBOARD: Byte = 2
    const val REPORT_ID_CONSUMER: Byte = 3

    // Mouse buttons
    const val MOUSE_BUTTON_NONE: Byte = 0
    const val MOUSE_BUTTON_LEFT: Byte = 1
    const val MOUSE_BUTTON_RIGHT: Byte = 2
    const val MOUSE_BUTTON_MIDDLE: Byte = 4
    const val MOUSE_BUTTON_BACK: Byte = 8
    const val MOUSE_BUTTON_FORWARD: Byte = 16

    // Keyboard modifiers
    const val MODIFIER_NONE: Byte = 0
    const val MODIFIER_LEFT_CTRL: Byte = 1
    const val MODIFIER_LEFT_SHIFT: Byte = 2
    const val MODIFIER_LEFT_ALT: Byte = 4
    const val MODIFIER_LEFT_GUI: Byte = 8      // Windows / Super / Command key
    const val MODIFIER_RIGHT_CTRL: Byte = 16
    const val MODIFIER_RIGHT_SHIFT: Byte = 32
    const val MODIFIER_RIGHT_ALT: Byte = 64    // AltGr
    const val MODIFIER_RIGHT_GUI: Byte = -128  // 0x80 signed byte

    // HID Keyboard Usages
    const val KEY_NONE: Byte = 0x00
    const val KEY_A: Byte = 0x04
    const val KEY_B: Byte = 0x05
    const val KEY_C: Byte = 0x06
    const val KEY_D: Byte = 0x07
    const val KEY_E: Byte = 0x08
    const val KEY_F: Byte = 0x09
    const val KEY_G: Byte = 0x0A
    const val KEY_H: Byte = 0x0B
    const val KEY_I: Byte = 0x0C
    const val KEY_J: Byte = 0x0D
    const val KEY_K: Byte = 0x0E
    const val KEY_L: Byte = 0x0F
    const val KEY_M: Byte = 0x10
    const val KEY_N: Byte = 0x11
    const val KEY_O: Byte = 0x12
    const val KEY_P: Byte = 0x13
    const val KEY_Q: Byte = 0x14
    const val KEY_R: Byte = 0x15
    const val KEY_S: Byte = 0x16
    const val KEY_T: Byte = 0x17
    const val KEY_U: Byte = 0x18
    const val KEY_V: Byte = 0x19
    const val KEY_W: Byte = 0x1A
    const val KEY_X: Byte = 0x1B
    const val KEY_Y: Byte = 0x1C
    const val KEY_Z: Byte = 0x1D

    const val KEY_1: Byte = 0x1E
    const val KEY_2: Byte = 0x1F
    const val KEY_3: Byte = 0x20
    const val KEY_4: Byte = 0x21
    const val KEY_5: Byte = 0x22
    const val KEY_6: Byte = 0x23
    const val KEY_7: Byte = 0x24
    const val KEY_8: Byte = 0x25
    const val KEY_9: Byte = 0x26
    const val KEY_0: Byte = 0x27

    const val KEY_ENTER: Byte = 0x28
    const val KEY_ESC: Byte = 0x29
    const val KEY_BACKSPACE: Byte = 0x2A
    const val KEY_TAB: Byte = 0x2B
    const val KEY_SPACE: Byte = 0x2C
    const val KEY_MINUS: Byte = 0x2D
    const val KEY_EQUAL: Byte = 0x2E
    const val KEY_LEFTBRACE: Byte = 0x2F
    const val KEY_RIGHTBRACE: Byte = 0x30
    const val KEY_BACKSLASH: Byte = 0x31
    const val KEY_SEMICOLON: Byte = 0x33
    const val KEY_APOSTROPHE: Byte = 0x34
    const val KEY_GRAVE: Byte = 0x35
    const val KEY_COMMA: Byte = 0x36
    const val KEY_DOT: Byte = 0x37
    const val KEY_SLASH: Byte = 0x38
    const val KEY_CAPSLOCK: Byte = 0x39

    const val KEY_F1: Byte = 0x3A
    const val KEY_F2: Byte = 0x3B
    const val KEY_F3: Byte = 0x3C
    const val KEY_F4: Byte = 0x3D
    const val KEY_F5: Byte = 0x3E
    const val KEY_F6: Byte = 0x3F
    const val KEY_F7: Byte = 0x40
    const val KEY_F8: Byte = 0x41
    const val KEY_F9: Byte = 0x42
    const val KEY_F10: Byte = 0x43
    const val KEY_F11: Byte = 0x44
    const val KEY_F12: Byte = 0x45

    const val KEY_SYSRQ: Byte = 0x46      // PrintScreen
    const val KEY_SCROLLLOCK: Byte = 0x47
    const val KEY_PAUSE: Byte = 0x48
    const val KEY_INSERT: Byte = 0x49
    const val KEY_HOME: Byte = 0x4A
    const val KEY_PAGEUP: Byte = 0x4B
    const val KEY_DELETE: Byte = 0x4C
    const val KEY_END: Byte = 0x4D
    const val KEY_PAGEDOWN: Byte = 0x4E
    const val KEY_RIGHT: Byte = 0x4F
    const val KEY_LEFT: Byte = 0x50
    const val KEY_DOWN: Byte = 0x51
    const val KEY_UP: Byte = 0x52

    // Consumer usages (16-bit)
    const val CONSUMER_PLAY_PAUSE: Short = 0x00CD
    const val CONSUMER_NEXT_TRACK: Short = 0x00B5
    const val CONSUMER_PREV_TRACK: Short = 0x00B6
    const val CONSUMER_VOLUME_MUTE: Short = 0x00E2
    const val CONSUMER_VOLUME_UP: Short = 0x00E9
    const val CONSUMER_VOLUME_DOWN: Short = 0x00EA

    val DESCRIPTOR: ByteArray = byteArrayOf(
        // ============================================
        // MOUSE REPORT DESCRIPTOR (Report ID 1)
        // ============================================
        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),        // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), REPORT_ID_MOUSE,     //   Report ID (1)
        0x09.toByte(), 0x01.toByte(),        //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(),        //   Collection (Physical)
        // 5 buttons (Left, Right, Middle, Back, Forward)
        0x05.toByte(), 0x09.toByte(),        //     Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),        //     Usage Minimum (Button 1)
        0x29.toByte(), 0x05.toByte(),        //     Usage Maximum (Button 5)
        0x15.toByte(), 0x00.toByte(),        //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //     Logical Maximum (1)
        0x95.toByte(), 0x05.toByte(),        //     Report Count (5)
        0x75.toByte(), 0x01.toByte(),        //     Report Size (1)
        0x81.toByte(), 0x02.toByte(),        //     Input (Data, Variable, Absolute)
        // 3 bits padding
        0x95.toByte(), 0x01.toByte(),        //     Report Count (1)
        0x75.toByte(), 0x03.toByte(),        //     Report Size (3)
        0x81.toByte(), 0x01.toByte(),        //     Input (Constant)
        // X, Y relative movement (-127 to 127)
        0x05.toByte(), 0x01.toByte(),        //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),        //     Usage (X)
        0x09.toByte(), 0x31.toByte(),        //     Usage (Y)
        0x15.toByte(), 0x81.toByte(),        //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),        //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),        //     Report Size (8)
        0x95.toByte(), 0x02.toByte(),        //     Report Count (2)
        0x81.toByte(), 0x06.toByte(),        //     Input (Data, Variable, Relative)
        // Wheel (Vertical scroll: -127 to 127)
        0x09.toByte(), 0x38.toByte(),        //     Usage (Wheel)
        0x15.toByte(), 0x81.toByte(),        //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),        //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),        //     Report Size (8)
        0x95.toByte(), 0x01.toByte(),        //     Report Count (1)
        0x81.toByte(), 0x06.toByte(),        //     Input (Data, Variable, Relative)
        // AC Pan (Horizontal scroll: -127 to 127)
        0x05.toByte(), 0x0C.toByte(),        //     Usage Page (Consumer Devices)
        0x0A.toByte(), 0x38.toByte(), 0x02.toByte(), // Usage (AC Pan)
        0x15.toByte(), 0x81.toByte(),        //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),        //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),        //     Report Size (8)
        0x95.toByte(), 0x01.toByte(),        //     Report Count (1)
        0x81.toByte(), 0x06.toByte(),        //     Input (Data, Variable, Relative)
        0xC0.toByte(),                      //   End Collection (Physical)
        0xC0.toByte(),                      // End Collection (Application)

        // ============================================
        // KEYBOARD REPORT DESCRIPTOR (Report ID 2)
        // ============================================
        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),        // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD,  //   Report ID (2)
        // Modifier keys (8 bits)
        0x05.toByte(), 0x07.toByte(),        //   Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(),        //   Usage Minimum (224 - Left Control)
        0x29.toByte(), 0xE7.toByte(),        //   Usage Maximum (231 - Right GUI)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),        //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),        //   Report Count (8)
        0x81.toByte(), 0x02.toByte(),        //   Input (Data, Variable, Absolute)
        // 1 byte reserved
        0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
        0x75.toByte(), 0x08.toByte(),        //   Report Size (8)
        0x81.toByte(), 0x01.toByte(),        //   Input (Constant)
        // LEDs (5 bits: Num Lock, Caps Lock, Scroll Lock, Compose, Kana)
        0x05.toByte(), 0x08.toByte(),        //   Usage Page (LEDs)
        0x19.toByte(), 0x01.toByte(),        //   Usage Minimum (1)
        0x29.toByte(), 0x05.toByte(),        //   Usage Maximum (5)
        0x95.toByte(), 0x05.toByte(),        //   Report Count (5)
        0x75.toByte(), 0x01.toByte(),        //   Report Size (1)
        0x91.toByte(), 0x02.toByte(),        //   Output (Data, Variable, Absolute)
        // 3 bits LED padding
        0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
        0x75.toByte(), 0x03.toByte(),        //   Report Size (3)
        0x91.toByte(), 0x01.toByte(),        //   Output (Constant)
        // 6 Key codes array (rollover)
        0x05.toByte(), 0x07.toByte(),        //   Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(),        //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x00.toByte(), // Usage Maximum (255)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(),        //   Report Size (8)
        0x95.toByte(), 0x06.toByte(),        //   Report Count (6)
        0x81.toByte(), 0x00.toByte(),        //   Input (Data, Array)
        0xC0.toByte(),                      // End Collection (Application)

        // ============================================
        // CONSUMER CONTROL REPORT DESCRIPTOR (Report ID 3)
        // ============================================
        0x05.toByte(), 0x0C.toByte(),        // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(),        // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER,  //   Report ID (3)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // Logical Maximum (0x03FF)
        0x19.toByte(), 0x00.toByte(),        //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // Usage Maximum (0x03FF)
        0x75.toByte(), 0x10.toByte(),        //   Report Size (16)
        0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
        0x81.toByte(), 0x00.toByte(),        //   Input (Data, Array)
        0xC0.toByte()                       // End Collection (Application)
    )
}
