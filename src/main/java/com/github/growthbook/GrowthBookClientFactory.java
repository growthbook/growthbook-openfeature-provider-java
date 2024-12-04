package com.github.growthbook;

import growthbook.sdk.java.multiusermode.GrowthBookClient;
import growthbook.sdk.java.multiusermode.configurations.Options;

/**
 * Factory class to create GrowthBookClient.
 */
public class GrowthBookClientFactory {

    /**
     * Returns a new instance of the GrowthBookClient.
     *
     * @return new GrowthBookClient with default options.
     */
    public static GrowthBookClient instance() {
        return new GrowthBookClient(Options.builder().build());
    }

    /**
     * Returns a new instance of the GrowthBookClient with given options.
     *
     * @param options to build the client.
     * @return new GrowthBookClient with given options.
     */
    public static GrowthBookClient instance(Options options) {
        return new GrowthBookClient(options);
    }
}
