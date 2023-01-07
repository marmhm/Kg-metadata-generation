package nl.cochez.query_processing.metadata;

import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementAssign;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementDataset;
import org.apache.jena.sparql.syntax.ElementExists;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementMinus;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementNotExists;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitor;

public abstract class GroupElementVisitor implements ElementVisitor {

    @Override
    public void visit(ElementTriplesBlock el) {
    }

    @Override
    public void visit(ElementPathBlock el) {
        // TODO Auto-generated method stub
    }

    @Override
    public void visit(ElementFilter el) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(ElementAssign el) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(ElementBind el) {
        // TODO Auto-generated method stub
        // System.out.println(el.toString());
    }

    @Override
    public abstract void visit(ElementData el);

    @Override
    public void visit(ElementUnion el) {
        // TODO Auto-generated method stub
        for(Element e : el.getElements())
            e.visit(this);
    }

    @Override
    public void visit(ElementOptional el) {
        // TODO Auto-generated method stub
        // System.out.println(el.getOptionalElement().toString());
        el.getOptionalElement().visit(this);
    }

    @Override
    public void visit(ElementGroup el) {
        // TODO Auto-generated method stub
        for(Element e : el.getElements())
            e.visit(this);
    }

    @Override
    public void visit(ElementDataset el) {
        // TODO Auto-generated method stub
        el.getElement().visit(this);
    }

    @Override
    public void visit(ElementNamedGraph el) {
        // TODO Auto-generated method stub
        el.getElement().visit(this);
    }

    @Override
    public void visit(ElementExists el) {
        // TODO Auto-generated method stub
        el.getElement().visit(this);
    }

    @Override
    public void visit(ElementNotExists el) {
        // TODO Auto-generated method stub
        el.getElement().visit(this);
    }

    @Override
    public void visit(ElementMinus el) {
        // TODO Auto-generated method stub
        el.getMinusElement().visit(this);
    }

    @Override
    public void visit(ElementService el) {
        // TODO Auto-generated method stub
        el.getElement().visit(this);
    }

    @Override
    public void visit(ElementSubQuery el) {
        // TODO Auto-generated method stub
        el.getQuery().getQueryPattern().visit(this);
    }
    
}
