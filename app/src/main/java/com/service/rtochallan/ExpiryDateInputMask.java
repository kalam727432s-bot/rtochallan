package com.service.rtochallan;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class ExpiryDateInputMask implements TextWatcher {

    private final EditText editText;
    private String current = "";

    public ExpiryDateInputMask(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        String input = s.toString();
        if (!input.equals(current)) {
            String cleanInput = input.replaceAll("[^\\d]", "");

            // Add leading zero if the first character is greater than 1
            if (cleanInput.length() > 0 && Character.getNumericValue(cleanInput.charAt(0)) > 1) {
                cleanInput = "0" + cleanInput;
            }

            StringBuilder formatted = new StringBuilder();
            int index = 0;

            for (char ch : cleanInput.toCharArray()) {
                if (index == 2) {
                    formatted.append("/");
                }
                formatted.append(ch);
                index++;
            }

            current = formatted.toString();
            editText.removeTextChangedListener(this);
            editText.setText(current);
            editText.setSelection(current.length());
            editText.addTextChangedListener(this);
        }
    }

}

