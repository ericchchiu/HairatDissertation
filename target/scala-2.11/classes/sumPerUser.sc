import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql._
import org.apache.spark
import org.apache.spark.sql.types._
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.regression.LinearRegression

val conf = new SparkConf().setAppName("retail").setMaster("local[*]")
val sc = new SparkContext(conf)
val spark = SparkSession.builder().appName("retail1").master("local[1*").getOrCreate()
import spark.implicits._

val dataRDD = sc.textFile("D:\\hairat dataset\\train datasets.csv")

val data0 = dataRDD.filter(line => (line.split(",")(0) != "User_ID")).
  map(line => {val arr = line.split(",");
    (arr(0), (arr(2), arr(3), arr(4), arr(5), arr(6), arr(7), arr(11).toInt))}).
  reduceByKey((a,b)=>(a._1, a._2, a._3, a._4, a._5, a._6, a._7+b._7)).
  map(a => (a._2._1, a._2._2, a._2._3, a._2._4, a._2._5, a._2._6, a._2._7))

data0.take(10).foreach(println)

val data = data0.map(line => ( if (line._1 == "F") 1.0 else 2.0,
      {val age = line._2
        if (age == "0-17") 1.0; else if (age == "18-25") 2.0;
        else if (age == "26-35") 3.0; else if (age == "36-45") 4.0;
        else if (age == "46-50" || age == "51-55") 5.0; else 6.0},
      line._3.toDouble,
      {val cityCat = line._4
        if (cityCat == "A") 1.0;
        else if (cityCat == "B") 2.0; else 3.0},
      {val stayCCityYr = line._5
        if (stayCCityYr == "4+") 5.0 else stayCCityYr.toDouble+1.0},
      if (line._6 == "0") 1.0 else 2.0,
      line._7.toDouble)
    )

/*
val data = dataRDD.filter(line => (line.split(",")(0) != "User_ID")).
                    map(line => ( if (line.split(",")(2) == "F") 1.0 else 2.0,
                      ({val age = line.split(",")(3)
                        if (age == "0-17") 1.0; else if (age == "18-25") 2.0;
                        else if (age == "26-35") 3.0; else if (age == "36-45") 4.0;
                        else if (age == "46-50" || age == "51-55") 5.0; else 6.0}),
                      (line.split(",")(4).toDouble),
                      ({val cityCat = line.split(",")(5)
                        if (cityCat == "A") 1.0;
                        else if (cityCat == "B") 2.0; else 3.0}),
                      ({val stayCCityYr = line.split(",")(6)
                        if (stayCCityYr == "4+") 5.0 else stayCCityYr.toDouble+1.0}),
                      (if (line.split(",")(7) == "0") 1.0 else 2.0),
                      (line.split(",")(11).toDouble))
                    )

case class TydiedData(Gender: Double, Age: Double, Occuplation: Double, City_Category: Double,
                      Stay_In_Current_City_Years: Double, Marital_Status: Double, Purchase: Double)*/

val data1 = data.toDF()

data1.printSchema()
data1.show

val data2 = data1.select(data1("_7").as("label"),$"_1",$"_2",$"_3",$"_4",$"_5",$"_6")


val assembler = new VectorAssembler().
  setInputCols(Array("_1","_2","_3","_4","_5","_6")).
    setOutputCol("features")

val output = assembler.transform(data2).select($"label",$"features")

output.show

val lr = new LinearRegression()

val lrModel = lr.fit(output)

val trainingSummary = lrModel.summary

trainingSummary.residuals.show()

trainingSummary.r2
