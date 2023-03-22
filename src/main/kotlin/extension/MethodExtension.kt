package extension

import org.apache.bcel.classfile.Method

val Method.fullDescription: String
    get() = "${this.name}:${this.signature}"
