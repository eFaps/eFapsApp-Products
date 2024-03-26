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
@EFapsUUID("748a6c1e-fb93-48b5-8ff2-65d6478a7519")
@EFapsApplication("eFapsApp-Products")
public class Storage
    extends Storage_Base
{

    /** The Constant CACHE_KEY. */
    public static final String CACHE_KEY = Storage_Base.CACHE_KEY;

    /**
     * Validate closure date.
     *
     * @param _parameter the parameter
     * @param _storageInst the storage inst
     * @param _date the date
     * @return true, if successful
     * @throws EFapsException the eFaps exception
     */
    public static boolean validateClosureDate(final Parameter _parameter,
                                              final Instance _storageInst,
                                              final DateTime _date)
        throws EFapsException
    {
        return Storage_Base.validateClosureDate(_parameter, _storageInst, _date);
    }

    /**
     * Gets the closure date.
     *
     * @param _parameter the parameter
     * @param _storageInst the storage inst
     * @return the closure date
     * @throws EFapsException the eFaps exception
     */
    public static DateTime getClosureDate(final Parameter _parameter,
                                          final Instance _storageInst)
        throws EFapsException
    {
        return Storage_Base.getClosureDate(_parameter, _storageInst);
    }

    /**
     * Get the default storage.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return instance of a storage
     * @throws EFapsException on error
     */
    public static Instance getDefaultStorage(final Parameter _parameter)
        throws EFapsException
    {
        return Storage_Base.getDefaultStorage(_parameter);
    }
}
