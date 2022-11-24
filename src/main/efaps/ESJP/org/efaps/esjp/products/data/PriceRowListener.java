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

import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.eql2.StmtFlag;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.data.IOnRow;
import org.efaps.esjp.data.jaxb.AbstractDef;
import org.efaps.esjp.data.jaxb.RowListener;
import org.efaps.util.EFapsException;
import org.joda.time.LocalDate;

@EFapsUUID("40eb428a-f673-4c0d-9771-fe2a6f9db8cb")
@EFapsApplication("eFapsApp-Products")
public class PriceRowListener
    implements IOnRow
{

    @Override
    public void run(final Parameter _parameter, final AbstractDef definition, final Instance instance,
                    final Map<String, Integer> _headers,
                    final String[] values, final Integer _idx)
        throws EFapsException
    {
        if (definition instanceof RowListener) {
            final String type = ((RowListener) definition).getProperty("PricelistType");
            final String currencyId = ((RowListener) definition).getProperty("CurrencyId");
            final String priceColumn = ((RowListener) definition).getProperty("PriceColumn");
            final var price = values[_headers.get(priceColumn)].trim();
            final var pricelistInst = EQL.builder().with(StmtFlag.TRIGGEROFF)
                            .insert(type)
                            .set(CIProducts.ProductPricelistAbstract.ProductAbstractLink, instance)
                            .set(CIProducts.ProductPricelistAbstract.ValidFrom, LocalDate.now().minusDays(1))
                            .set(CIProducts.ProductPricelistAbstract.ValidUntil, LocalDate.now().plusYears(10))
                            .execute();

            EQL.builder()
                            .insert(CIProducts.ProductPricelistPosition)
                            .set(CIProducts.ProductPricelistPosition.ProductPricelist, pricelistInst)
                            .set(CIProducts.ProductPricelistPosition.CurrencyId, currencyId)
                            .set(CIProducts.ProductPricelistPosition.Price, price)
                            .execute();
        }
    }
}
