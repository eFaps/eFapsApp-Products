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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.graphql.AbstractDataFetcher;
import org.efaps.esjp.products.Product;
import org.efaps.util.EFapsException;
import org.efaps.util.OIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.execution.DataFetcherResult.Builder;
import graphql.schema.DataFetchingEnvironment;

@EFapsUUID("49125b23-83e4-4a00-8a93-99ebf62b8877")
@EFapsApplication("eFapsApp-Products")
public class CreateBatchMutation
    extends AbstractDataFetcher
{

    private static final Logger LOG = LoggerFactory.getLogger(CreateBatchMutation.class);

    @Override
    public Object get(final DataFetchingEnvironment environment)
        throws Exception
    {
        final var resultBldr = DataFetcherResult.newResult();
        final var props = getProperties(environment);

        final var batchNameVariable = props.getProperty("BatchNameVariable", "batchName");
        final var batchName = environment.<String>getArgument(batchNameVariable);

        final var eval = EQL.builder().print().query(CIProducts.ProductAbstract)
                        .where()
                        .attribute(CIProducts.ProductAbstract.Name).eq(batchName)
                        .select()
                        .oid()
                        .evaluate();
        if (eval.next()) {
            resultBldr.error(GraphqlErrorBuilder.newError(environment)
                            .message("A product with '" + batchName + "' already exists")
                            .build());
        } else {
            final var productInstance = evalProductInstance(environment, props, resultBldr);
            final Map<String, Object> map = new HashMap<>();
            map.put("Name", batchName);
            final var parameter = ParameterUtil.instance();
            final Instance batchInst = new Product().cloneProduct(parameter, productInstance,
                            CIProducts.ProductBatch.getType(), map, true);
            EQL.builder().insert(CIProducts.StoreableProductAbstract2Batch)
                            .set(CIProducts.StoreableProductAbstract2Batch.FromLink, productInstance)
                            .set(CIProducts.StoreableProductAbstract2Batch.ToLink, batchInst)
                            .execute();
            resultBldr.data(batchInst.getOid());
        }
        return resultBldr.build();
    }

    protected Instance evalProductInstance(final DataFetchingEnvironment environment,
                                           final Properties props,
                                           Builder<Object> resultBldr)
        throws EFapsException
    {
        Instance ret = null;
        LOG.info("Evaluating instance the batch will be created for");
        final var productOidVariable = props.getProperty("ProductOidVariable", "productOid");
        final var productOid = environment.<String>getArgument(productOidVariable);
        if (OIDUtil.isOID(productOid)) {
            final var productInstance = Instance.get(productOid);
            if (InstanceUtils.isKindOf(productInstance, CIProducts.StoreableProductAbstract)) {
                ret = productInstance;
            } else {
                resultBldr.error(GraphqlErrorBuilder.newError(environment)
                                .message("OID '" + productOid + "' is not a valid product.")
                                .build());
            }
        } else {
            final var productNameVariable = props.getProperty("ProductNameVariable", "productName");
            final var productName = environment.<String>getArgument(productNameVariable);
            final var eval = EQL.builder().print().query(CIProducts.ProductAbstract)
                            .where()
                            .attribute(CIProducts.ProductAbstract.Name).eq(productName)
                            .select()
                            .oid()
                            .evaluate();
            if (eval.next()) {
                final var productInst = eval.inst();
                if (InstanceUtils.isKindOf(productInst, CIProducts.StoreableProductAbstract)) {
                    ret = productInst;
                } else {
                    resultBldr.error(GraphqlErrorBuilder.newError(environment)
                                    .message("Instance found for  '" + productName + "' is not a valid product.")
                                    .build());
                }
            } else {
                resultBldr.error(GraphqlErrorBuilder.newError(environment)
                                .message("Product" + productName + "' cannot be found.")
                                .build());
            }
        }
        return ret;
    }

}
