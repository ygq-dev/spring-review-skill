package com.example.style;

public class ComplexService {
    public void process(int[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] > 0) {
                for (int j = 0; j < items[i]; j++) {
                    if (j % 2 == 0) {
                        if (j > 10) {
                            System.out.println(j);
                        }
                    }
                }
            }
        }
    }
}