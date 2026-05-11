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

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.datetime.DateAndTimeUtils;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("9ef6e2db-3c79-421a-aa37-ca87d5189b0d")
@EFapsApplication("eFapsApp-Products")
public class ProductPrice
{

    private static final Logger LOG = LoggerFactory.getLogger(ProductPrice.class);

    public static Price getPrice(final Instance productInstance,
                                 final CIType priceListType)
        throws EFapsException
    {
        return getPrice(productInstance, priceListType, null);
    }

    public static Price getPrice(final Instance productInstance,
                                 final CIType priceListType,
                                 final Instance priceGroupInst)
        throws EFapsException
    {
        Price ret = null;
        final var now = DateAndTimeUtils.withTimeAtStartOfDay();

        final var nestedQuery = EQL.builder().nestedQuery(priceListType)
                        .where()
                        .attribute(CIProducts.ProductPricelistAbstract.ProductAbstractLink).eq(productInstance)
                        .and()
                        .attribute(CIProducts.ProductPricelistAbstract.ValidFrom).lessOrEq(now)
                        .and()
                        .attribute(CIProducts.ProductPricelistAbstract.ValidUntil).greaterOrEq(now)
                        .up();

        final var query = EQL.builder().print().query(CIProducts.ProductPricelistPosition)
                        .where()
                        .attribute(CIProducts.ProductPricelistPosition.ProductPricelist).in(nestedQuery);

        if (InstanceUtils.isKindOf(priceGroupInst, CIProducts.PriceGroupAbstract)) {
            query.and().attribute(CIProducts.ProductPricelistPosition.PriceGroupLink).eq(priceGroupInst);
        }

        final var eval = query
                        .select()
                        .attribute(CIProducts.ProductPricelistPosition.CurrencyId,
                                        CIProducts.ProductPricelistPosition.Price)
                        .evaluate();
        if (eval.next()) {
            ret = new Price().setAmount(eval.get(CIProducts.ProductPricelistPosition.Price))
                            .setCurrencyId(eval.get(CIProducts.ProductPricelistPosition.CurrencyId));
        }
        if (eval.next()) {
            LOG.warn("More than one valid price found for product: {}", productInstance);
        }
        return ret;
    }

    public static class Price
    {

        private BigDecimal amount;
        private Long currencyId;

        public Long getCurrencyId()
        {
            return currencyId;
        }

        public Price setCurrencyId(Long currencyId)
        {
            this.currencyId = currencyId;
            return this;
        }

        public BigDecimal getAmount()
        {
            return amount;
        }

        public Price setAmount(BigDecimal amount)
        {
            this.amount = amount;
            return this;
        }
    }
}
