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

import java.util.Properties;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.graphql.BaseUpdateMutation;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.DataFetchingEnvironment;

@EFapsUUID("33c08471-7744-402e-96ea-d8dadfc821b3")
@EFapsApplication("eFapsApp-Products")
public class UpdateProductMutation
    extends BaseUpdateMutation
{

    private static final Logger LOG = LoggerFactory.getLogger(UpdateProductMutation.class);

    @Override
    protected Instance evalInstance(final DataFetchingEnvironment environment,
                                    final Properties props)
        throws EFapsException
    {
        Instance ret = null;
        LOG.info("Evaluating instance to update");
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
}
