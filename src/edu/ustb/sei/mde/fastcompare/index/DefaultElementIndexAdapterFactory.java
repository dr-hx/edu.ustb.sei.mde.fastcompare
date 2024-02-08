package edu.ustb.sei.mde.fastcompare.index;

public class DefaultElementIndexAdapterFactory extends ElementIndexAdapterFactory {

    @Override
    protected ElementIndexAdapter createAdapter(int id) {
        return new ElementIndexAdapter(id);
    }
    
}
