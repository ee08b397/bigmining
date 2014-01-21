package edu.fiu.cs.bigmining.linearregression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

public class LinearRegressionMapper extends
    Mapper<LongWritable, VectorWritable, LongWritable, VectorWritable> {

  /* a sparse vector contains the weight updates */
  private long count;
  private int featureDimension;

  private double learningRate;
  private double biasUpdate;
  private double[] weightUpdates;

  private LinearRegressionModel model;

  @Override
  public void setup(Context context) {
    Configuration conf = context.getConfiguration();
    this.count = 0;
    this.featureDimension = conf.getInt("feature.dimension", 1);
    this.learningRate = Double.parseDouble(conf.get("learning.rate") == null ? conf
        .get("learning.rate") : "0.1");

    this.biasUpdate = 0;
    this.weightUpdates = new double[this.featureDimension];

    // read the metaData
    Map<String, String> metaData = new HashMap<String, String>();
    model = new LinearRegressionModel(featureDimension, metaData);
  }

  @Override
  /**
   * Train the model with full-batch stochastic regression.
   */
  public void map(LongWritable key, VectorWritable value, Context context) throws IOException {
    ++count;
    Vector vec = value.get();
    double expected = vec.get(featureDimension);
    double actual = model.predict(vec);

    // update bias
    this.biasUpdate -= learningRate * (actual - expected);

    // update each weight
    for (int i = 0; i < featureDimension; ++i) {
      this.weightUpdates[i] -= learningRate * (actual - expected) * vec.get(i);
    }
  }

  /**
   * Write local updates to reducer.
   * Local update: \delta w = learningRate * \frac{1}{count} \sigma_{count} (y - t) * x
   */
  public void cleanup(Context context) throws IOException, InterruptedException {
    Vector vec = new DenseVector(1 + this.featureDimension);
    vec.set(0, this.biasUpdate);
    for (int i = 0; i < featureDimension; ++i) {
      vec.set(i + 1, this.weightUpdates[i]);
    }

    vec.divide(count);
    context.write(new LongWritable(0), new VectorWritable(vec));
  }

}
