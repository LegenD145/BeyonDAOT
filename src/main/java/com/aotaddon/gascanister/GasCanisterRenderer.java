package com.aotaddon.gascanister;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GasCanisterRenderer extends GeoBlockRenderer<GasCanisterBlockEntity> {
    public GasCanisterRenderer() {
        super(new GasCanisterGeoModel());
    }
}
