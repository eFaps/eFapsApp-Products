/*
 * Copyright 2003 - 2015 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.efaps.esjp.products.util;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("3cad9ef8-22e6-4b3f-9a97-3aa984d8d6c6")
@EFapsApplication("eFapsApp-Products")
public interface ProductsSettings
{
    /**
     * ProductsSettings.BASE string.
     */
    String BASE = "org.efaps.products.";

    /**
     * Boolean(true/false).<br/>
     * Activate the image menu.
     */
    String ACTIVATEIMAGE = ProductsSettings.BASE + "ActivateImages";

    /**
     * Boolean(true/false).<br/>
     * Activate the individual management menu.
     */
    String ACTIVATEINDIVIDUAL = ProductsSettings.BASE + "ActivateIndividual";

    /**
     * Boolean(true/false).<br/>
     * Activate the menu for updating Product Prices on mass.
     */
    String ACTIVATEPRICEMASSUPDATE = ProductsSettings.BASE + "ActivatePriceMassUpdate";

    /**
     * Boolean(true/false).<br/>
     * Activate the menu for updating Product Prices on mass.
     */
    String ACTIVATEFAMILIES = ProductsSettings.BASE + "ActivateFamilies";

    /**
     * Properties.<br/>
     * Properties for image
     */
    String IMAGEPROPERTIES = ProductsSettings.BASE + "ImagesProperties";

    /**
     * Link to a default warehouse instance. Can also be used as a link like
     * "...DefaultWareHouse.Key"
     */
    String DEFAULTWAREHOUSE =  ProductsSettings.BASE + "DefaultWareHouse";

    /**
     * Link to a default warehouse instance.
     */
    String DEFAULTSTORAGEGROUP = ProductsSettings.BASE + "StorageGroup4ProductBOM";

    /**
     * Link to a default dimension.
     */
    String DEFAULTDIMENSION = ProductsSettings.BASE + "DefaultDimension";

    /**
     * Properties for the LastMovementReport.
     */
    String LASTMOVEREP = ProductsSettings.BASE + "LastMovementReport";

    /**
     * Length of the Suffix for families.
     */
    String FAMILYSUFFIXLENGTH = ProductsSettings.BASE + "FamilySuffixRegex";

    /**
     * Properties. Can be concatenated. Setting for Variant Attributes.
     */
    String VARIANTCONFIG = ProductsSettings.BASE + "VariantConfiguration";
}
