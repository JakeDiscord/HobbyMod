package io.github.jakediscord.hobbymod.astronomy;

/** Shared discovery policy; all motorized mount requests also pass it on the server. */
public final class ObservationAccess {
    public static boolean discovered(ObservationJournal journal,int id){
        var entry=journal.get(id);return entry!=null && entry.observations>0 && entry.completeness>0;
    }
    public static boolean canPoint(ObservationJournal journal,SkyCatalog.Object target){return target!=null && discovered(journal,target.id());}
    public static boolean canTrack(ObservationJournal journal,SkyCatalog.Object target,boolean observatory){return observatory && canPoint(journal,target) && target.kind()==SkyCatalog.Kind.PLANET;}
    private ObservationAccess(){}
}
