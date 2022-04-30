
package jp.minecraftuser.ecoframework.db;

import java.text.MessageFormat;

/**
 * 適当なO/Rマッパー探した方が絶対楽だと思います
 * @author ecolight
 */
public enum CTYPE {
    /**
     * 長整数処理
     */
    LONG {
        @Override
        public String get(JdbcBase base) {
            if (base instanceof JdbcSqlite) {
                return "INTEGER";
            } else if (base instanceof JdbcMySQL) {
                return "BIGINT";
            }
            return null;
        }

        @Override
        public String primary(JdbcBase base, String key) {
            return mf.format(new String[]{key});
        }
    },

    /**
     * 浮動小数点数処理
     */
    FLOAT {
        @Override
        public String get(JdbcBase base) {
            if (base instanceof JdbcSqlite) {
                return "REAL";
            } else if (base instanceof JdbcMySQL) {
                return "FLOAT";
            }
            return null;
        }

        @Override
        public String primary(JdbcBase base, String key) {
            return mf.format(new String[]{key});
        }
    },

    /**
     * 文字列型処理
     */
    STRING {
        @Override
        public String get(JdbcBase base) {
            if (base instanceof JdbcSqlite) {
                return "TEXT";
            } else if (base instanceof JdbcMySQL) {
                return "TEXT";
            }
            return null;
        }

        @Override
        public String primary(JdbcBase base, String key) {
            if (base instanceof JdbcSqlite) {
                return mf.format(new String[]{key});
            } else if (base instanceof JdbcMySQL) {
                return mf_size.format(new String[]{key});
            }
            return null;
        }
    },

    /**
     * 文字列型処理
     */
    STRING_KEY {
        @Override
        public String get(JdbcBase base) {
            if (base instanceof JdbcSqlite) {
                return "TEXT";
            } else if (base instanceof JdbcMySQL) {
                return "VARCHAR(255)";
            }
            return null;
        }

        @Override
        public String primary(JdbcBase base, String key) {
            if (base instanceof JdbcSqlite) {
                return mf.format(new String[]{key});
            } else if (base instanceof JdbcMySQL) {
                return mf_size.format(new String[]{key});
            }
            return null;
        }
    },

    /**
     * バイナリ型処理
     */
    BLOB {
        @Override
        public String get(JdbcBase base) {
            if (base instanceof JdbcSqlite) {
                return "BLOB";
            } else if (base instanceof JdbcMySQL) {
                return "LONGBLOB";
            }
            return null;
        }

        @Override
        public String primary(JdbcBase base, String key) {
            if (base instanceof JdbcSqlite) {
                return mf.format(new String[]{key});
            } else if (base instanceof JdbcMySQL) {
                return mf_size.format(new String[]{key});
            }
            return null;
        }
    },
    /**
     * バイナリ型処理
     */
    AUTOINCREMENT {
        @Override
        public String get(JdbcBase base) {
            if (base instanceof JdbcSqlite) {
                return "AUTOINCREMENT";
            } else if (base instanceof JdbcMySQL) {
                return "AUTO_INCREMENT";
            }
            return null;
        }

        @Override
        public String primary(JdbcBase base, String key) {
            return key;
        }
    },
    ;
    private static final MessageFormat mf = new MessageFormat("({0})");
    private static final MessageFormat mf_size = new MessageFormat("({0}(255))");

    /**
     * 各DB型の該当型名を返す
     * @param base DB型
     * @return 型名
     */
    public abstract String get(JdbcBase base);

    /**
     * PrimaryKey指定する際の命名規則で返す サイズはFramework向けに固定
     * @param base DB型
     * @param key キー名
     * @return 修飾キー名
     */
    public abstract String primary(JdbcBase base, String key);
}
