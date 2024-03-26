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

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("37de213e-c112-4dec-801f-7f25c8a89fb8")
@EFapsApplication("eFapsApp-Products")
public class ProductFamily
    extends ProductFamily_Base
{
    /** The cachekey. */
    public static final String CACHEKEY = ProductFamily_Base.CACHEKEY;

    /**
     * Gets the descendants.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _famInst the fam inst
     * @return the descendants
     * @throws EFapsException on error
     */
    public static List<Instance> getDescendants(final Parameter _parameter,
                                                final Instance _famInst)
        throws EFapsException
    {
        return ProductFamily_Base.getDescendants(_parameter, _famInst);
    }

    /**
     * Gets the family instance for code.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _code the code
     * @return the family instance4 code
     * @throws EFapsException on error
     */
    public static Instance getFamilyInstance4Code(final Parameter _parameter,
                                                  final String _code)
        throws EFapsException
    {
        return ProductFamily_Base.getFamilyInstance4Code(_parameter, _code);
    }
}
