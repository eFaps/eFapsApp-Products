/*
 * Copyright 2003 - 2009 The eFaps Team
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

package org.efaps.esjp.products;

import java.util.UUID;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
public enum Products
{
    /** Products_ProductAbstract.*/
    ABSTRACT("29c61210-5e44-453b-a171-95993c95da36"),
    /** Products_BinLocation.*/
    BINLOCATION("757f2c00-5aee-466f-b813-9d60482069d4"),
    /** Products_ProductComponent.*/
    COMPONENT("baeb0a19-9e52-4025-a401-6f6a14b61cef"),
    /** Products_ProductCost.*/
    COST("3d99fa55-571f-4904-8f38-155922e08d1c"),
    /** Products_Inventory.*/
    INVENTORY("de159d46-cafd-48b9-9113-bb40ce7ef1ee"),
    /** Products_ProductPricelistAbstract.*/
    PRICELISTABSTRACT("aa914fe4-dba3-4514-a364-c296d357cec7"),
    /** Products_ProductPricelistMinRetail.*/
    PRICELISTMINRETAIL("62f8a170-c773-4fd2-977f-e3668a5357e0"),
    /** Products_ProductPricelistPurchase.*/
    PRICELISTPURCHASE("d0aec333-157e-4f6a-bb8c-ea31f2706d40"),
    /** Products_ProductPricelistRetail.*/
    PRICELISTRETAIL("0eefc758-0be6-4c40-8cf9-a4f88986f76e"),
    /** Products_ProductStandart.*/
    STANDART("75fb0a3d-c4a5-4d65-b481-4873155ba677"),
    /** Products_ProductStandart2Unique. */
    STANADRT2UNIQUE("a41b9cc7-837a-4178-914b-8a189950ab24"),
    /** Products_ProductTextPosition. */
    TEXTPOSITION("56a93eed-eedc-44be-a548-26fd33b195bf"),
    /** Products_TransactionAbstract.*/
    TRANSABSTRACT("59c6c464-4334-427c-8fe5-98810b0e032a"),
    /** Products_TransactionInbound.*/
    TRANSIN("85140a94-5221-4778-b3c3-c4114891013d"),
    /** Products_TransactionOutbound.*/
    TRANSOUT("113b4e56-9225-4952-8b13-94d538c97d74"),
    /** Products_TransactionReservationInbound.*/
    TRANSRESIN("0a028f80-c93d-498b-b9d5-f1c51062799b"),
    /** Products_TransactionReservationOutbound.*/
    TRANSRESOUT("d3a17607-0f69-4def-ab9f-7aa62e4dfbca"),
    /** Products_ProductUnique.*/
    UNIQUE("65b14d43-022b-4b86-b18b-f705204672f1"),
    /** Products_ProductUniqueAbstract.*/
    UNIQUEABSTRACT("b1efc9c1-5693-49a5-b17c-e3229898a561"),
    /** Products_ProductUniqueComponent.*/
    UNIQUECOMPONENT("f9ed8878-1680-494f-af85-b94a89a39d33");

    /**
     * UUID for the Type.
     */
    private final UUID uuid;


    /**
     * @param _uuid string for the uuid
     */
    private Products(final String _uuid)
    {
        this.uuid = UUID.fromString(_uuid);
    }

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
    public UUID getUuid()
    {
        return this.uuid;
    }
}
