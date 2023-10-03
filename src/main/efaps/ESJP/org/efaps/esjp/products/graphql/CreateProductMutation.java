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

import java.util.Map;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.graphql.BaseCreateMutation;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult.Builder;
import graphql.schema.DataFetchingEnvironment;

@EFapsUUID("7671eccc-777d-4a97-8667-ea00c678aef2")
@EFapsApplication("eFapsApp-Products")
public class CreateProductMutation
    extends BaseCreateMutation
{

    private static final Logger LOG = LoggerFactory.getLogger(CreateProductMutation.class);

    @Override
    protected boolean validateValues(final DataFetchingEnvironment environment,
                                     final Map<String, Object> values,
                                     final Builder<Object> resultBldr)
        throws EFapsException
    {
        var ret = super.validateValues(environment, values, resultBldr);
        if (ret) {
            LOG.debug("Validating name for product");
            if (values.containsKey("attribute[Name]")) {
                final String name = (String) values.get("attribute[Name]");
                final var eval = EQL.builder().print().query(CIProducts.ProductAbstract)
                                .where()
                                .attribute(CIProducts.ProductAbstract.Name).eq(name)
                                .select()
                                .oid()
                                .evaluate();
                if (eval.next()) {
                    resultBldr.error(GraphqlErrorBuilder.newError(environment)
                                    .message("A product with '" + name + "' already exists")
                                    .build());
                    ret = false;
                }
            }
        }
        return ret;
    }
}
