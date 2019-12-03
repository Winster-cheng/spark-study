import org.apache.spark.{SparkConf, SparkContext}
import scala.util.control.Breaks._

object median {
  def main (args: Array[String]) {
    val conf =new SparkConf().setAppName("Median").setMaster("local[1]")
    val sc=new SparkContext(conf)
    //通过textFile读入的是字符串型，所以要进行类型转换
    val data =sc.textFile("median/src/main/scala/data").flatMap(x=>x.split(' ')).map(x=>x.toInt)
    //将数据分为4组，当然我这里的数据少
    val  mappeddata =data.map(x=>(x/4,x)).sortByKey()
    //p_count为每个分组的个数
    val p_count =data.map(x=>(x/4,1)).reduceByKey(_+_).sortByKey()
    p_count.foreach(println)
    //p_count是一个RDD，不能进行Map集合操作，所以要通过collectAsMap方法将其转换成scala的集合
    val scala_p_count=p_count.collectAsMap()
    //根据key值得到value值
    println(scala_p_count(0))
    //sum_count是统计总的个数，不能用count(),因为会得到多少个map对。
    val sum_count = p_count.map(x=>x._2).sum().toInt
    println(sum_count)
    var temp =0//中值所在的区间累加的个数
    var temp2=0//中值所在区间的前面所有的区间累加的个数
    var index=0//中值的区间
    var mid= 0
    if(sum_count%2!=0){
      mid =sum_count/2+1//中值在整个数据的偏移量
    }
    else{
      mid =sum_count/2
    }
    val pcount=p_count.count()
    breakable{
      for(i <- 0 to pcount.toInt-1){
        temp =temp + scala_p_count(i)
        temp2 =temp-scala_p_count(i)
        if(temp>=mid){
          index=i
          break
        }
      }
    }
    println(mid+" "+index+" "+temp+" "+temp2)
    //中位数在桶中的偏移量
    val offset =mid-temp2
    //takeOrdered它默认可以将key从小到大排序后，获取rdd中的前n个元素
    val result =mappeddata.filter(x=>x._1==index).takeOrdered(offset)
    println(result(offset-1)._2)
    sc.stop()
  }

}
