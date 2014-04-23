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

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("96b4a9bc-bfcf-41fa-9c03-4dbc6279cf63")
@EFapsRevision("$Rev$")
public final class Products
{
    /**
     * Singelton.
     */
    private Products()
    {
    }

    public enum ProductIndividual
        implements IEnum
    {
        NONE, INDIVIDUAL, BATCH;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    /**
     * @return the SystemConfigruation for Sales
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        // Products-Configuration
        return SystemConfiguration.get(UUID.fromString("e53cd705-e463-47dc-a400-4ace4ed72071"));
    }
}
