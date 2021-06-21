/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ml4j.tensor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvmpy.symbolictensors.Size;
import org.ml4j.autograd.BackwardConfig;
import org.mockito.MockitoAnnotations;

/**
 * A base test for Tensor implementations.
 * 
 * @author Michael Lavelle
 *
 */
public abstract class TensorTestBase<T extends Tensor<T, D>, D> extends TestBase<T, D> {

    @Test
    public void test_example() {

        var a = createGradValue(-4f, true).name_("a");

        var b = createGradValue(2.0f, true).name_("b");

        var c = a.add(b);

        var d = a.mul(b).add(b.mul(b).mul(b));

        c = c.add(c.add(1));

        c = c.add(one().add(c).sub(a));

        d = d.add(d.mul(2).add(b.add(a).relu()));

        d = d.add(d.mul(3).add(b.sub(a).relu()));

        var e = c.sub(d);

        var f = e.mul(e);

        var g = f.div(2f);

        g = g.add(ten().div(f));

        assertEquals(createData(24.70f), g.data().get());

        g.backward();

        assertEquals(createData(138.83f), a.grad().data().get());

        assertEquals(createData(645.58f), b.grad().data().get());
    }

    @Test
    public void test_hessian_vector() {

        var x = createGradValue(0.5f, true).name_("x");

        var y = createGradValue(0.6f, true).name_("y");

        var z = x.mul(x).add(y.mul(x).add(y.mul(y))).name_("z");

        var two = createGradValue(2, true).name_("two");

        z.backward(new BackwardConfig().with_keep_graph(true));

        var xGradAfterFirstBackward = x.grad();
        var yGradAfterFirstBackward = y.grad();

        assertEquals(createData(1.6f), xGradAfterFirstBackward.data().get());

        assertEquals(createData(1.7f), yGradAfterFirstBackward.data().get());

        var x_grad = createGradValue(add(mul(x.data().get(),2f),y.data().get()), false);
        var y_grad = createGradValue(add(x.data().get(), mul(y.data().get(),2f)), false);

        var grad_sum = x.grad().mul(two).add(y.grad());

        grad_sum.backward(new BackwardConfig());

        var xGradAfterSecondBackward = x.grad();
        var yGradAfterSecondBackward = y.grad();

        Assert.assertSame(xGradAfterFirstBackward, xGradAfterSecondBackward);
        assertEquals(createData(6.6f), xGradAfterSecondBackward.data().get());

        Assert.assertSame(yGradAfterFirstBackward, yGradAfterSecondBackward);

        assertEquals(createData(5.7f), yGradAfterSecondBackward.data().get());

        var x_hv = 5;
        var y_hv = 4;

        Assert.assertArrayEquals(x.grad().getDataAsFloatArray(), x_grad.add(createGradValue(x_hv, false)).getDataAsFloatArray(), 0.001f);
        Assert.assertArrayEquals(y.grad().getDataAsFloatArray(), y_grad.add(createGradValue(y_hv, false)).getDataAsFloatArray(), 0.001f);
    }

    @Test
    public void test_sum() {

        var a = createGradValue(-4f, true, new Size(2, 2)).name_("a");

        var c = a.sum();

        assertEquals(createData(-16f, new Size()), c.data().get());

        c.backward();

        assertEquals(createData(1, new Size(2, 2)), a.grad().data().get());

    }

    @Test
    public void test_get_row() {

        var a = createGradValue(-4f, true, new Size(2, 2)).name_("a");

        var c = a.getTensor(-1, 0);

        assertEquals(createData(-4, new Size(2, 1)), c.data().get());
    }

    @Test
    public void testMatMul() {
        var left = createGradValue(-1, true, new Size(new Size(2, 128), new Size(512))).name_("a");
        var right = createGradValue(1, true, new Size(512, 65)).name_("a");

        var result = left.matmul(right);

        result.backward();

        Assert.assertNotNull(left.grad());
        Assert.assertNotNull(right.grad());
    }
}
