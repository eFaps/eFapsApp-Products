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
package org.efaps.esjp.products.graphql;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.graphql.BaseUpdateMutation;
import org.efaps.esjp.products.PriceMassUpdate;
import org.efaps.esjp.products.PriceMassUpdate_Base.MassUpdateEntry;
import org.efaps.util.EFapsException;
import org.efaps.util.OIDUtil;
import org.efaps.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult.Builder;
import graphql.schema.DataFetchingEnvironment;

@EFapsUUID("51d7b7ee-a080-47e8-936f-0fe4532b659e")
@EFapsApplication("eFapsApp-Products")
public class UpdatePriceMutation
    extends BaseUpdateMutation
{

    private static final Logger LOG = LoggerFactory.getLogger(UpdatePriceMutation.class);

    @Override
    protected Instance evalInstance(final DataFetchingEnvironment environment,
                                    final Properties props)
        throws EFapsException
    {
        Instance ret = null;
        LOG.info("Evaluating product instance for UpdatePriceMutation");
        final var nameVariableName = props.getProperty("NameVariable");
        if (nameVariableName == null) {
            super.evalInstance(environment, props);
        }
        final var inputValue = environment.<String>getArgument(nameVariableName);
        final var eval = EQL.builder().print().query(CIProducts.ProductAbstract)
                        .where()
                        .attribute(CIProducts.ProductAbstract.Name).eq(inputValue)
                        .select()
                        .oid()
                        .evaluate();
        if (eval.next()) {
            ret = eval.inst();
        }
        return ret;
    }

    @Override
    protected Instance executeStmt(final DataFetchingEnvironment environment,
                                   final Builder<Object> resultBldr,
                                   final Instance instance,
                                   final Map<String, Object> values)
        throws EFapsException
    {
        if (InstanceUtils.isKindOf(instance, CIProducts.ProductAbstract)) {
            final var properties = getProperties(environment);
            final var priceListTypeStr = properties.getProperty("priceListType",
                            CIProducts.ProductPricelistRetail.uuid.toString());
            Type priceListType;
            if (UUIDUtil.isUUID(priceListTypeStr)) {
                priceListType = Type.get(UUID.fromString(priceListTypeStr));
            } else {
                priceListType = Type.get(priceListTypeStr);
            }
            final var priceVar = properties.getProperty("PriceVariable", "price");
            final var price = values.get(priceVar);

            final var currencyVar = properties.getProperty("CurrencyVariable", "currency");
            final var currencyVal = values.get(currencyVar);
            Instance currency = null;
            if (currencyVal instanceof String) {
                if (OIDUtil.isOID((String) currencyVal)) {
                    currency = Instance.get((String) currencyVal);
                } else {
                    currency = CurrencyInst.find((String) currencyVal).get().getInstance();
                }
            } else if (currencyVal instanceof Long) {
                currency = CurrencyInst.get((Long) currencyVal).getInstance();
            }
            final var entry = new MassUpdateEntry()
                            .setProductInstance(instance)
                            .setNewPrice((Number) price)
                            .setCurrencyInstance(currency);
            final var parameter = ParameterUtil.instance();
            new PriceMassUpdate().execute(parameter, Collections.singleton(entry), priceListType);
            resultBldr.data(instance.getOid());
        } else {
            resultBldr.error(GraphqlErrorBuilder.newError(environment)
                            .message("Instance evaluated is not a valid product")
                            .build());
        }
        return instance;

    }
}
