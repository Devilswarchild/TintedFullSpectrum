package com.devilswarchild.tintedfullspectrum;

// Marker for any item eligible for the generic RecolorRecipe and whose placed block should pick up
// its color from TintColorComponent on placement (see TintableBlocks#applyPlacementColor). Adding a
// new tintable material is just implementing this on its BlockItem -- see TintableBlockItem /
// TintableStandingAndWallBlockItem for ready-made wrappers around the two vanilla BlockItem shapes
// this mod currently needs.
public interface TintableItem {
}
