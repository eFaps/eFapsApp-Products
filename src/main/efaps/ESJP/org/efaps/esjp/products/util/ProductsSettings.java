/*
 * Copyright 2003 - 2013 The eFaps Team
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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */


package org.efaps.esjp.products.util;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("3cad9ef8-22e6-4b3f-9a97-3aa984d8d6c6")
@EFapsRevision("$Rev$")
public interface ProductsSettings
{
    /**
     * Boolean(true/false).<br/>
     * Activate the image menu.
     */
    String ACTIVATEIMAGE = "org.efaps.products.ActivateImages";

    /**
     * Properties.<br/>
     * Properties for image
     */
    String IMAGEPROPERTIES = "org.efaps.products.ImagesProperties";

    /**
     * Boolean(true/false).<br/>
     * Activate the menu for updating Product Prices on mass.
     */
    String ACTIVATEPRICEMASSUPDATE = "org.efaps.products.ActivatePriceMassUpdate";

    /**
     * Link to a default warehouse instance
     */
    String DEFAULTWAREHOUSE = "org.efaps.products.DefaultWareHouse";

    /**
     * Link to a default warehouse instance
     */
    String DEFAULTSTORAGEGROUP = "org.efaps.products.StorageGroup4ProductBOM";

    /**
     * Link to a default warehouse instance
     */
    String DEFAULTDIMENSION = "org.efaps.products.DefaultDimension";

    /**
     * Boolean(true/false).<br/>
     * Activate the individual management menu.
     */
    String ACTIVATEINDIVIDUAL = "org.efaps.products.ActivateIndividual";

}
