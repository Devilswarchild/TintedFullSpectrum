package com.devilswarchild.tintedfullspectrum;

// Implemented by any block entity whose color can be set by right-clicking a Colored Dye on it
// (see ColoredDyeItem#useOn). Any future tintable block (wool, terracotta, glass, ...) just needs
// to implement this to pick up dye support for free.
public interface TintableBlockEntity {
    int getColor();

    void setColor(int rgb);
}
