# 各级缓冲配置例子
集合形式

元素格式如下：
     int level;//流量池级别有：0全国;-1常规；1省；2市；3区县；4乡镇
    int capacity;//一个流量池的缓冲容量，最大能缓冲的条目数
    
    
## json例子：

```json
[
    {
        "level":0,
        "capacity":50000
    },
    {
        "level":-1,
        "capacity":20000
    },
    {
        "level":1,
        "capacity":40000
    },
    {
        "level":2,
        "capacity":30000
    },
    {
        "level":3,
        "capacity":20000
    },
    {
        "level":4,
        "capacity":10000
    }
]
```