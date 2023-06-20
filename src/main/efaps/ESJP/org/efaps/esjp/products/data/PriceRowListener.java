/*
 * Copyright 2003 - 2022 The eFaps Team
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
package org.efaps.esjp.products.data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.data.IOnRow;
import org.efaps.esjp.data.jaxb.AbstractDef;
import org.efaps.esjp.data.jaxb.RowListener;
import org.efaps.esjp.products.PriceMassUpdate;
import org.efaps.esjp.products.PriceMassUpdate_Base.MassUpdateEntry;
import org.efaps.util.EFapsException;



@EFapsUUID("40eb428a-f673-4c0d-9771-fe2a6f9db8cb")
@EFapsApplication("eFapsApp-Products")
public class PriceRowListener
    implements IOnRow
{

    @Override
    public void run(final Parameter parameter,
                    final AbstractDef definition,
                    final Instance productInstance,
                    final Map<String, Integer> headers,
                    final String[] values,
                    final Integer idx)
        throws EFapsException
    {
        if (definition instanceof RowListener) {
            final String typeName = ((RowListener) definition).getProperty("PricelistType");
            final String currencyOid = ((RowListener) definition).getProperty("CurrencyOid");
            final String priceColumn = ((RowListener) definition).getProperty("PriceColumn");
            // optional
            final String priceGroupOid = ((RowListener) definition).getProperty("PriceGroupOid");

            final var price = new BigDecimal(values[headers.get(priceColumn)].trim());
            final var priceGroupInst = Instance.get(priceGroupOid);
            final var entry = new MassUpdateEntry()
                            .setProductInstance(productInstance)
                            .setNewPrice(price)
                            .setCurrencyInstance(Instance.get(currencyOid));
            if (priceGroupInst.isValid()) {
                entry.setPriceGroupInstance(priceGroupInst);
            }
            new PriceMassUpdate().execute(parameter, Collections.singletonList(entry), Type.get(typeName));
        }
    }
}
