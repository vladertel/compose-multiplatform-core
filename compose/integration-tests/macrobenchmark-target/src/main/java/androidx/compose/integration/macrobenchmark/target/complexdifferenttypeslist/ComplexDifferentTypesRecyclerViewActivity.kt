/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.rv.SquadAdapter
import androidx.compose.integration.macrobenchmark.target.databinding.ActivityRecyclerviewBinding
import androidx.recyclerview.widget.LinearLayoutManager

class ComplexDifferentTypesRecyclerViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapper = SquadMapper()

        val binding = ActivityRecyclerviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = SquadAdapter()
        binding.recycler.adapter = adapter
        binding.recycler.layoutManager = LinearLayoutManager(this)

        adapter.updateItems(mapper.map())
    }
}
