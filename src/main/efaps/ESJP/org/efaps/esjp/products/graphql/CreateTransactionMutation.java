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
package org.efaps.esjp.products.graphql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Properties;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.graphql.BaseCreateMutation;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.util.EFapsException;
import org.efaps.util.OIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.execution.DataFetcherResult.Builder;
import graphql.schema.DataFetchingEnvironment;

@EFapsUUID("33afc98f-2651-4a9e-a73a-e72fd7758993")
@EFapsApplication("eFapsApp-Products")
public class CreateTransactionMutation
    extends BaseCreateMutation
{

    private static final Logger LOG = LoggerFactory.getLogger(CreateTransactionMutation.class);

    @Override
    public Object get(final DataFetchingEnvironment environment)
        throws Exception
    {
        final var resultBldr = DataFetcherResult.newResult();
        final var props = getProperties(environment);

        final var values = evalArgumentValues(environment, props);

        final var storageInstance = evalStorageInstance(environment, props, values, resultBldr);
        final var productInstance = evalProductInstance(environment, props, values, resultBldr);

        final var descriptionVariable = props.getProperty("DescriptionVariable", "description");
        final var description = values.get(descriptionVariable);

        final var uoMIdVariable = props.getProperty("UoMIdVariable", "uomId");
        final var uoMId = values.get(uoMIdVariable);

        final var quantityVariable = props.getProperty("QuantityVariable", "quantity");
        final BigDecimal quantity = (BigDecimal) values.get(quantityVariable);

        final var dateVariable = props.getProperty("DateVariable", "date");
        final LocalDate date = (LocalDate) values.get(dateVariable);

        if (!resultBldr.hasErrors()) {
            final var transactionType = evalTransactionType(environment, productInstance, quantity, resultBldr);

            if (transactionType != null) {
                final var insertInst = EQL.builder().insert(transactionType)
                                .set(CIProducts.TransactionAbstract.Product, productInstance)
                                .set(CIProducts.TransactionAbstract.Storage, storageInstance)
                                .set(CIProducts.TransactionAbstract.Description, description)
                                .set(CIProducts.TransactionAbstract.Quantity, quantity.abs())
                                .set(CIProducts.TransactionAbstract.UoM, uoMId)
                                .set(CIProducts.TransactionAbstract.Date, date == null ? LocalDate.now() : date)
                                .execute();
                resultBldr.data(insertInst.getOid());

                if (transactionType.equals(CIProducts.TransactionIndividualInbound)
                                || transactionType.equals(CIProducts.TransactionIndividualOutbound)) {
                    final var eval = EQL.builder().print(productInstance)
                                    .linkfrom(CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract)
                                    .linkto(CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract)
                                    .instance().first().as("prodInst")
                                    .evaluate();
                    final Instance parentProductInst = eval.get("prodInst");

                    final var parentTransactionType = transactionType.equals(CIProducts.TransactionIndividualInbound)
                                    ? CIProducts.TransactionInbound
                                    : CIProducts.TransactionOutbound;
                    EQL.builder().insert(parentTransactionType)
                                    .set(CIProducts.TransactionAbstract.Product, parentProductInst)
                                    .set(CIProducts.TransactionAbstract.Storage, storageInstance)
                                    .set(CIProducts.TransactionAbstract.Description, description)
                                    .set(CIProducts.TransactionAbstract.Quantity, quantity.abs())
                                    .set(CIProducts.TransactionAbstract.UoM, uoMId)
                                    .set(CIProducts.TransactionAbstract.Date, date == null ? LocalDate.now() : date)
                                    .execute();
                }
            }
        }
        return resultBldr.build();
    }

    protected CIType evalTransactionType(final DataFetchingEnvironment environment,
                                         final Instance productInstance,
                                         final BigDecimal quantity,
                                         final Builder<Object> resultBldr)
        throws EFapsException
    {
        CIType ret = null;
        if (InstanceUtils.isKindOf(productInstance, CIProducts.ProductIndividualAbstract)) {
            ret = quantity.compareTo(BigDecimal.ZERO) > 0 ? CIProducts.TransactionIndividualInbound
                            : CIProducts.TransactionIndividualOutbound;
        } else if (Products.ACTIVATEINDIVIDUAL.get()) {
            final var eval = EQL.builder().print(productInstance)
                            .attribute(CIProducts.StoreableProductAbstract.Individual)
                            .evaluate();
            final var individual = eval.get(CIProducts.StoreableProductAbstract.Individual);
            if (ProductIndividual.BATCH.equals(individual) || ProductIndividual.INDIVIDUAL.equals(individual)) {
                resultBldr.error(GraphqlErrorBuilder.newError(environment)
                                .message("Product Instance found for  '" + productInstance
                                                + "' is not a valid product.")
                                .build());
            } else {
                ret = quantity.compareTo(BigDecimal.ZERO) > 0 ? CIProducts.TransactionInbound
                                : CIProducts.TransactionOutbound;
            }
        } else {
            ret = quantity.compareTo(BigDecimal.ZERO) > 0 ? CIProducts.TransactionInbound
                            : CIProducts.TransactionOutbound;
        }
        return ret;
    }

    protected Instance evalProductInstance(final DataFetchingEnvironment environment,
                                           final Properties props,
                                           final Map<String, Object> values,
                                           final Builder<Object> resultBldr)
        throws EFapsException
    {
        Instance ret = null;
        LOG.info("Evaluating Product instance to use");
        final var productOidVariable = props.getProperty("ProductOidVariable", "productOid");
        final var productOid = (String) values.get(productOidVariable);
        if (OIDUtil.isOID(productOid)) {
            ret = Instance.get(productOid);
        }
        if (!InstanceUtils.isKindOf(ret, CIProducts.StoreableProductAbstract)) {
            resultBldr.error(GraphqlErrorBuilder.newError(environment)
                            .message("No valid Product Instance found for  '" + productOid)
                            .build());
        }
        return ret;
    }

    protected Instance evalStorageInstance(final DataFetchingEnvironment environment,
                                           final Properties props,
                                           final Map<String, Object> values,
                                           final Builder<Object> resultBldr)
        throws EFapsException
    {
        Instance ret = null;
        LOG.info("Evaluating Storage instance to use");
        final var storageOidVariable = props.getProperty("StorageOidVariable", "storageOid");
        final String storageOid = (String) values.get(storageOidVariable);
        if (OIDUtil.isOID(storageOid)) {
            ret = Instance.get(storageOid);

        }
        if (!InstanceUtils.isKindOf(ret, CIProducts.StorageAbstract)) {
            resultBldr.error(GraphqlErrorBuilder.newError(environment)
                            .message("No valid Storage Instance found for  '" + storageOid)
                            .build());
        }
        return ret;
    }
}
