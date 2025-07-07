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
package org.efaps.esjp.products.rest.modules;

import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@EFapsUUID("69b79f5b-b571-400f-8a1f-68c8c2e9c9f7")
@EFapsApplication("eFapsApp-Products")
@Path("/ui/modules/product-family")
public class ProductFamilyController
{

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getFamilyTree(@QueryParam("productOid") final String productOid)
        throws EFapsException
    {
        String current = null;
        if (productOid != null) {
            final var eval = EQL.builder().print(productOid)
                            .linkto(CIProducts.ProductAbstract.ProductFamilyLink).oid().as("famOid")
                            .evaluate();
            if (eval.next()) {
                current = eval.get("famOid");
            }
        }

        final var dto = ProductFamilyResponseDto.builder()
                        .withCurrent(current)
                        .withFamilies(loadFamilyTree())
                        .build();

        return Response.ok(dto)
                        .build();
    }

    public List<ProductFamilyDto> loadFamilyTree()
        throws EFapsException
    {
        final var dtos = new ArrayList<ProductFamilyDto>();
        final var eval = EQL.builder().print().query(CIProducts.ProductFamilyRoot)
                        .select()
                        .attribute(CIProducts.ProductFamilyRoot.Name, CIProducts.ProductFamilyRoot.CodePart)
                        .evaluate();
        while (eval.next()) {
            dtos.add(ProductFamilyDto.builder()
                            .withOid(eval.inst().getOid())
                            .withLabel(eval.get(CIProducts.ProductFamilyRoot.Name))
                            .withCodePart(eval.get(CIProducts.ProductFamilyRoot.CodePart))
                            .withChildren(loadChildren(eval.inst()))
                            .build());
        }
        return dtos;
    }

    public List<ProductFamilyDto> loadChildren(final Instance parentInst)
        throws EFapsException
    {
        final var dtos = new ArrayList<ProductFamilyDto>();
        final var eval = EQL.builder().print().query(CIProducts.ProductFamilyStandart)
                        .where()
                        .attribute(CIProducts.ProductFamilyStandart.ParentLink).eq(parentInst)
                        .select()
                        .attribute(CIProducts.ProductFamilyStandart.Name, CIProducts.ProductFamilyStandart.CodePart)
                        .evaluate();
        while (eval.next()) {
            dtos.add(ProductFamilyDto.builder()
                            .withOid(eval.inst().getOid())
                            .withLabel(eval.get(CIProducts.ProductFamilyStandart.Name))
                            .withCodePart(eval.get(CIProducts.ProductFamilyStandart.CodePart))
                            .withChildren(loadChildren(eval.inst()))
                            .withParentOid(parentInst.getOid())
                            .build());
        }
        return dtos;
    }
}
