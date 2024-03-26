/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.products;

import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("699f77eb-a2bc-49c2-b777-8b17b0acfa13")
@EFapsApplication("eFapsApp-Products")
public class Inventory
    extends Inventory_Base
{

    /**
     * Gets the inventory for product.
     *
     * @param _parameter the _parameter
     * @param _storageInst the _storage inst
     * @param _prodInstance the _prod instance
     * @return the inventory4 product
     * @throws EFapsException on error
     */
    public static InventoryBean getInventory4Product(final Parameter _parameter,
                                                     final Instance _storageInst,
                                                     final Instance _prodInstance)
        throws EFapsException
    {
        return Inventory_Base.getInventory4Product(_parameter, _storageInst, _prodInstance);
    }

    /**
     * Gets the inventory4 product.
     *
     * @param _parameter the _parameter
     * @param _storageInst the _storage inst
     * @param _date the _date
     * @param _prodInstance the _prod instance
     * @return the inventory4 product
     * @throws EFapsException on error
     */
    public static InventoryBean getInventory4Product(final Parameter _parameter,
                                                     final Instance _storageInst,
                                                     final DateTime _date,
                                                     final Instance _prodInstance)
        throws EFapsException
    {
        return Inventory_Base.getInventory4Product(_parameter, _storageInst, _date, _prodInstance);
    }

    /**
     * Gets the inventory 4 products.
     *
     * @param _parameter the _parameter
     * @param _storageInst the _storage inst
     * @param _prodInstances the _prod instances
     * @return the inventory4 products
     * @throws EFapsException on error
     */
    public static Map<Instance, InventoryBean> getInventory4Products(final Parameter _parameter,
                                                                     final Instance _storageInst,
                                                                     final Instance... _prodInstances)
        throws EFapsException
    {
        return Inventory_Base.getInventory4Products(_parameter, _storageInst, _prodInstances);
    }

    /**
     * Gets the inventory 4 products.
     *
     * @param _parameter the _parameter
     * @param _storageInst the _storage inst
     * @param _date the _date
     * @param _prodInstances the _prod instances
     * @return the inventory4 products
     * @throws EFapsException on error
     */
    public static Map<Instance, InventoryBean> getInventory4Products(final Parameter _parameter,
                                                                     final Instance _storageInst,
                                                                     final DateTime _date,
                                                                     final Instance... _prodInstances)
        throws EFapsException
    {
        return Inventory_Base.getInventory4Products(_parameter, _storageInst, _date, _prodInstances);
    }
}
