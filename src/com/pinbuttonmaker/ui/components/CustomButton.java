package com.pinbuttonmaker.ui.components;

import javax.swing.JButton;

import com.pinbuttonmaker.ui.UIStyles;

public class CustomButton extends JButton {
    public CustomButton(String text) {
        super(text);
        UIStyles.stylePrimaryButton(this);
    }
}
