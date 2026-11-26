package app;

/** Satu nilai UTS: mata pelajaran dan skornya. */
final class GradeRecord {
    final String subject;
    final double score;

    GradeRecord(String subject, double score) {
        this.subject = subject;
        this.score = score;
    }
}
