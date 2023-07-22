/*
 * Copyright 2003 - 2018 The eFaps Team
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

package org.efaps.esjp.products.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.stmt.selection.Evaluator;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.products.rest.dto.ProductDTO;
import org.efaps.util.EFapsException;

@EFapsUUID("234d2afc-724e-4315-b63b-b0ada02eb5b6")
@EFapsApplication("eFapsApp-Products")
public abstract class Products_Base
    extends AbstractRest
{
    /**
     * Gets the products.
     *
     * @return the products
     * @throws EFapsException the eFaps exception
     */
    public Response getProducts()
        throws EFapsException
    {
        checkAccess();
        final Evaluator eval = EQL.builder()
            .print()
            .query(CIProducts.ProductAbstract)
            .select()
            .attribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description)
            .stmt()
            .evaluate();
        final List<ProductDTO> products = new ArrayList<>();
        while (eval.next()) {
            products.add(ProductDTO.builder()
                .withOid(eval.inst().getOid())
                .withName(eval.get(CIProducts.ProductAbstract.Name))
                .withDescription(eval.get(CIProducts.ProductAbstract.Description))
                .build());
        }
        final Response ret = Response.ok()
                        .entity(products)
                        .build();
        return ret;
    }
}
