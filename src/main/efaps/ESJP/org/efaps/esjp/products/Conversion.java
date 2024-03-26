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

import java.math.BigDecimal;

import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.products.util.ConversionType;
import org.efaps.util.EFapsException;

@EFapsUUID("f4ccdc1a-3a81-4b11-bb4f-548afe085e9b")
@EFapsApplication("eFapsApp-Products")
public class Conversion
    extends Conversion_Base
{

    public static ConversionValue convert(final ConversionType conversionType, final Instance _productInstance,
                                          final BigDecimal _quantity, final UoM _uoM)
        throws EFapsException
    {
        return Conversion_Base.convert(conversionType, _productInstance, _quantity, _uoM);
    }
}
