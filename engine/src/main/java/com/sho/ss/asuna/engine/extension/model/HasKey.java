package com.sho.ss.asuna.engine.extension.model;


import com.sho.ss.asuna.engine.core.utils.Experimental;

/**
 * Interface to be implemented by page mode.<br>
 * Can be used to identify a page model, or be used as name of file storing the object.<br>
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
@Experimental
public interface HasKey {

    /**
     *
     *
     * @return key
     */
    public String key();
}
