/*
 * Copyright 2003 - 2023 The eFaps Team
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
package org.efaps.esjp.products.listener;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

@EFapsUUID("bc114c22-2ec6-42b6-aa05-cf718a126bee")
@EFapsApplication("eFapsApp-Products")
public interface IPriceListListener
    extends IEsjpListener
{

    default boolean groupApplies(final Parameter _parameter,
                                 final Instance _priceGroupInstance)
        throws EFapsException
    {
        return false;
    }

    @Override
    default int getWeight()
    {
        return 0;
    }
}
