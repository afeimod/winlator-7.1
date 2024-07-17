.class public Lcom/example/datainsert/winlator/all/Test;
.super Ljava/lang/Object;
.source "Test.java"


# static fields
.field private static final TAG:Ljava/lang/String; = "MyTest"


# direct methods
.method public constructor <init>()V
    .registers 1

    .prologue
    .line 10
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static log(I)V
    .registers 4
    .param p0, "i"    # I

    .prologue
    .line 13
    const-string v0, "MyTest"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "log: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    .line 14
    return-void
.end method

.method public static log(Ljava/lang/String;)V
    .registers 4
    .param p0, "s"    # Ljava/lang/String;

    .prologue
    .line 16
    const-string v0, "MyTest"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "log: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    .line 17
    return-void
.end method


# virtual methods
.method public stringToLong(Ljava/lang/String;)J
    .registers 4
    .param p1, "str"    # Ljava/lang/String;

    .prologue
    .line 25
    invoke-static {p1}, Ljava/lang/Long;->parseUnsignedLong(Ljava/lang/String;)J

    move-result-wide v0

    return-wide v0
.end method

.method public testCallLog()V
    .registers 2

    .prologue
    .line 20
    const/4 v0, 0x3

    .line 21
    .local v0, "i":I
    invoke-static {v0}, Lcom/example/datainsert/winlator/all/Test;->log(I)V

    .line 22
    return-void
.end method
