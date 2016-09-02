/*
 * Copyright (c) 2015 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package org.brunel.build.d3.diagrams;

import org.brunel.action.Param;
import org.brunel.build.d3.D3Interaction;
import org.brunel.build.d3.element.D3ElementBuilder;
import org.brunel.build.d3.element.ElementDetails;
import org.brunel.build.d3.element.ElementRepresentation;
import org.brunel.build.d3.element.GeomAttribute;
import org.brunel.build.util.ModelUtil;
import org.brunel.build.util.ScriptWriter;
import org.brunel.data.Dataset;
import org.brunel.model.VisSingle;

class Grid extends Bubble {

    private int rows = 0, columns = 0;                  // size of the grid (0 -> "choose for me")
    private double aspect = 1;                          // desired aspect ratio of the grid cells

    Grid(VisSingle vis, Dataset data, D3Interaction interaction, ScriptWriter out) {
        super(vis, data, interaction, out);

        for (Param p : vis.tDiagramParameters) {
            String s = p.asString();
            if (s.equals("aspect") && p.hasModifiers())
                aspect = p.firstModifier().asDouble();
            else if (s.equals("rows") && p.hasModifiers())
                rows = (int) p.firstModifier().asDouble();
            if (s.equals("columns") && p.hasModifiers())
                columns = (int) p.firstModifier().asDouble();
        }
    }

    public void writeDiagramEnter() {
        // Nothing
    }

    public ElementDetails initializeDiagram() {
        out.comment("Define hierarchy and grid data structures");

        makeHierarchicalTree();

        out.add("var gridLabels = BrunelD3.gridLayout(tree, [geom.inner_width, geom.inner_height], "
                + rows + ", " + columns+ ", " + aspect + ")").endStatement();

        ElementRepresentation representation = ModelUtil.getElementSymbol(vis) == null
                ? ElementRepresentation.spaceFillingCircle : ElementRepresentation.symbol;
        return ElementDetails.makeForDiagram(vis, representation, "point", "tree.leaves()");
    }

    public void writeDefinition(ElementDetails details) {
        // Overwrite the details with the values generated by the diagram algorithm
        details.x.center = GeomAttribute.makeFunction("scale_x(d.x)");
        details.y.center = GeomAttribute.makeFunction("scale_y(d.y)");

        // So this is a bit of a hack to replace the "default size" with the calculated size
        // but it should be fine unless the definition of the default size changes
        String replacement = details.overallSize.definition().replace("geom.default_point_size", "(scale_x(d.r) - scale_x(0))");
        details.overallSize = GeomAttribute.makeFunction(replacement);

        // Simple circles, with classes defined for CSS
        out.addChained("attr('class', function(d) { return (d.children ? 'element L' + d.depth : 'leaf element " + element.name() + "') })");

        D3ElementBuilder.definePointLikeMark(details, vis, out);
        addAestheticsAndTooltips(details);
        labelBuilder.addGridLabels();
    }

    public boolean needsDiagramExtras() {
        return true;
    }
}