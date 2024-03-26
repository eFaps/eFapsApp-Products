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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.graphql.definition.ObjectDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

@EFapsUUID("8a284642-04b2-440e-9b90-ba0868583488")
@EFapsApplication("eFapsApp-Products")
public class TransactionDataFetcher
    implements DataFetcher<Object>
{

    private static final Logger LOG = LoggerFactory.getLogger(TransactionDataFetcher.class);

    private static String PRODUCT_OID_KEY = "productOid";
    private static String STORAGE_OID_KEY = "storageOid";
    private static String QUANTITY_KEY = "quantity";
    private static String DATE_KEY = "date";

    @Override
    public Object get(final DataFetchingEnvironment environment)
        throws Exception
    {
        String result = null;
        LOG.debug("Running TransactionDataFetcher with: {}", environment);
        final var mutationName = environment.getFieldDefinition().getName();
        final Optional<ObjectDef> mutationObjectDefOpt = environment.getGraphQlContext()
                        .getOrEmpty("mutation");
        if (mutationObjectDefOpt.isPresent()) {
            final var fieldDef = mutationObjectDefOpt.get().getFields().get(mutationName);
            if (fieldDef != null) {
                final var argumentDefs = fieldDef.getArguments();

                final var productOidDef = argumentDefs.stream().filter(argument -> {
                    return argument.getKey().equals(PRODUCT_OID_KEY);
                }).findFirst();
                final var storageOidDef = argumentDefs.stream().filter(argument -> {
                    return argument.getKey().equals(STORAGE_OID_KEY);
                }).findFirst();
                final var quantityDef = argumentDefs.stream().filter(argument -> {
                    return argument.getKey().equals(QUANTITY_KEY);
                }).findFirst();
                final var dateDef = argumentDefs.stream().filter(argument -> {
                    return argument.getKey().equals(DATE_KEY);
                }).findFirst();

                if (productOidDef.isEmpty() || storageOidDef.isEmpty() || quantityDef.isEmpty()) {
                    return null;
                } else {
                    final String productOid = (String) environment.getArguments().get(productOidDef.get().getName());
                    final String storageOid = (String) environment.getArguments().get(storageOidDef.get().getName());
                    final BigDecimal quantity = (BigDecimal) environment.getArguments()
                                    .get(quantityDef.get().getName());
                    LocalDate date = null;
                    if (dateDef.isPresent()) {
                        date = (LocalDate) environment.getArguments().get(dateDef.get().getName());
                    }

                    final var productInst = Instance.get(productOid);
                    if (InstanceUtils.isKindOf(productInst, CIProducts.StoreableProductAbstract)) {
                        final var productEval = EQL.builder().print(productInst)
                                        .attribute(CIProducts.ProductAbstract.DefaultUoM)
                                        .evaluate();
                        final var uoM = productEval.get(CIProducts.ProductAbstract.DefaultUoM);

                        final var type = quantity.compareTo(BigDecimal.ZERO) > 0 ? CIProducts.TransactionInbound
                                        : CIProducts.TransactionOutbound;

                        final var transInst = EQL.builder().insert(type)
                                        .set(CIProducts.TransactionAbstract.Storage, Instance.get(storageOid))
                                        .set(CIProducts.TransactionAbstract.Product, productInst)
                                        .set(CIProducts.TransactionAbstract.Quantity, quantity.abs())
                                        .set(CIProducts.TransactionAbstract.UoM, uoM)
                                        .set(CIProducts.TransactionAbstract.Date, date == null ? LocalDate.now() : date)
                                        .execute();
                        result = transInst.getOid();
                    }
                }
            }
        }
        return result;
    }

}
